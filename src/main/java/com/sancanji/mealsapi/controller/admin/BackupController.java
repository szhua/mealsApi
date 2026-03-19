package com.sancanji.mealsapi.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.sql.DataSource;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Controller
@RequestMapping("/admin/backup")
@RequiredArgsConstructor
public class BackupController {

    private final DataSource dataSource;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    private static final List<String> TABLES = Arrays.asList(
            "t_user", "t_category", "t_dish", "t_fridge", "t_plan", "t_plan_day", "t_streak", "t_check_in"
    );

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadBackup() throws IOException {
        // 创建临时文件
        Path tempFile = Files.createTempFile("backup-", ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile.toFile()))) {
            // 1. 导出数据库
            exportDatabase(zos);

            // 2. 导出图片
            exportImages(zos);
        }

        // 生成文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "meals_backup_" + timestamp + ".zip";

        // 返回文件
        InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile.toFile()));
        long fileSize = Files.size(tempFile);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"")
                .contentLength(fileSize)
                .body(resource);
    }

    private void exportDatabase(ZipOutputStream zos) throws IOException {
        StringBuilder sql = new StringBuilder();

        try (Connection conn = dataSource.getConnection()) {
            // 导出每个表的数据
            for (String table : TABLES) {
                exportTableData(conn, table, sql);
            }

            // 导出序列值（PostgreSQL需要重置序列）
            for (String table : TABLES) {
                sql.append("SELECT setval('").append(table).append("_id_seq', (SELECT COALESCE(MAX(id), 1) FROM ")
                        .append(table).append("));\n");
            }

        } catch (SQLException e) {
            log.error("导出数据库失败", e);
            throw new IOException("导出数据库失败: " + e.getMessage(), e);
        }

        // 添加到ZIP
        ZipEntry entry = new ZipEntry("backup/data.sql");
        zos.putNextEntry(entry);
        zos.write(sql.toString().getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void exportTableData(Connection conn, String table, StringBuilder sql) throws SQLException {
        // 获取列名
        List<String> columns = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }

        if (columns.isEmpty()) {
            log.warn("表 {} 不存在或没有列", table);
            return;
        }

        String columnList = String.join(", ", columns);
        String query = "SELECT " + columnList + " FROM " + table + " ORDER BY id";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                sql.append("INSERT INTO ").append(table).append(" (").append(columnList).append(") VALUES (");

                List<String> values = new ArrayList<>();
                for (int i = 0; i < columns.size(); i++) {
                    Object value = rs.getObject(i + 1);
                    values.add(formatValue(value));
                }

                sql.append(String.join(", ", values));
                sql.append(");\n");
            }
        }

        sql.append("\n");
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof java.sql.Date) {
            return "'" + value.toString() + "'";
        }
        if (value instanceof Timestamp) {
            return "'" + value.toString() + "'";
        }
        // 字符串类型，转义单引号
        String str = value.toString().replace("'", "''");
        return "'" + str + "'";
    }

    private void exportImages(ZipOutputStream zos) throws IOException {
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            log.info("上传目录不存在: {}", uploadPath);
            return;
        }

        // 遍历上传目录
        try (var stream = Files.walk(uploadDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String relativePath = uploadDir.relativize(file).toString().replace("\\", "/");
                            ZipEntry entry = new ZipEntry("backup/images/" + relativePath);
                            zos.putNextEntry(entry);
                            Files.copy(file, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            log.warn("添加文件到备份失败: {}", file, e);
                        }
                    });
        }
    }

    @PostMapping("/upload")
    public String uploadBackup(@RequestParam("file") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "请选择备份文件");
            return "redirect:/admin/users";
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".zip")) {
            redirectAttributes.addFlashAttribute("error", "请上传ZIP格式的备份文件");
            return "redirect:/admin/users";
        }

        Path tempDir = null;
        try {
            // 创建临时目录
            tempDir = Files.createTempDirectory("backup-restore-");

            // 解压ZIP文件
            unzipFile(file.getInputStream(), tempDir);

            // 恢复数据库
            Path sqlFile = tempDir.resolve("backup/data.sql");
            if (Files.exists(sqlFile)) {
                restoreDatabase(sqlFile);
            } else {
                throw new IOException("备份文件中未找到data.sql");
            }

            // 恢复图片
            Path imagesDir = tempDir.resolve("backup/images");
            if (Files.exists(imagesDir)) {
                restoreImages(imagesDir);
            }

            redirectAttributes.addFlashAttribute("success", "数据恢复成功");
            log.info("数据恢复完成");

        } catch (Exception e) {
            log.error("数据恢复失败", e);
            redirectAttributes.addFlashAttribute("error", "数据恢复失败: " + e.getMessage());
        } finally {
            // 清理临时文件
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }

        return "redirect:/admin/users";
    }

    private void unzipFile(InputStream is, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetFile = targetDir.resolve(entry.getName());

                // 安全检查：防止路径遍历攻击
                if (!targetFile.normalize().startsWith(targetDir.normalize())) {
                    throw new IOException("非法的ZIP文件路径: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetFile);
                } else {
                    Files.createDirectories(targetFile.getParent());
                    Files.copy(zis, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private void restoreDatabase(Path sqlFile) throws SQLException, IOException {
        String sqlContent = Files.readString(sqlFile, StandardCharsets.UTF_8);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 关闭外键检查
            stmt.execute("SET session_replication_role = replica");

            // 清空所有表
            for (String table : TABLES) {
                try {
                    stmt.execute("TRUNCATE TABLE " + table + " CASCADE");
                } catch (SQLException e) {
                    log.warn("清空表 {} 失败: {}", table, e.getMessage());
                }
            }

            // 执行恢复SQL
            String[] statements = sqlContent.split(";");
            for (String sql : statements) {
                sql = sql.trim();
                if (!sql.isEmpty() && !sql.startsWith("SELECT setval")) {
                    try {
                        stmt.execute(sql);
                    } catch (SQLException e) {
                        log.warn("执行SQL语句失败: {}", sql.substring(0, Math.min(50, sql.length())), e);
                    }
                }
            }

            // 重置序列
            for (String table : TABLES) {
                try {
                    stmt.execute("SELECT setval('" + table + "_id_seq', (SELECT COALESCE(MAX(id), 1) FROM " + table + "))");
                } catch (SQLException e) {
                    log.warn("重置序列 {}_id_seq 失败: {}", table, e.getMessage());
                }
            }

            // 恢复外键检查
            stmt.execute("SET session_replication_role = DEFAULT");

            log.info("数据库恢复完成");
        }
    }

    private void restoreImages(Path imagesDir) throws IOException {
        Path targetDir = Paths.get(uploadPath);
        Files.createDirectories(targetDir);

        try (var stream = Files.walk(imagesDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(source -> {
                        try {
                            String relativePath = imagesDir.relativize(source).toString();
                            Path target = targetDir.resolve(relativePath);
                            Files.createDirectories(target.getParent());
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            log.warn("恢复图片失败: {}", source, e);
                        }
                    });
        }
    }

    private void deleteDirectory(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除临时文件失败: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("清理临时目录失败: {}", dir, e);
        }
    }
}