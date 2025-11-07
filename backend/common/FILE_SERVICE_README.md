# 文件服务使用说明

## 概述

文件服务位于 `common` 模块中，提供统一的文件上传、下载、删除等功能。文件服务**不管理数据库**，只负责文件存储，返回文件路径供业务模块存储到自己的数据库中。

## 核心类

### 1. FileService
文件服务主类，提供以下功能：
- `uploadFile()` - 上传文件
- `readFile()` - 读取文件
- `deleteFile()` - 删除文件
- `fileExists()` - 检查文件是否存在
- `getFileSize()` - 获取文件大小

### 2. FileUtils
文件工具类，提供：
- `toInputStream()` - 将MultipartFile转换为InputStream
- `isImage()` - 检查是否为图片
- `isVideo()` - 检查是否为视频
- `formatFileSize()` - 格式化文件大小

## 配置

在 `config-server/src/main/resources/configs/application.yml` 中配置：

```yaml
file:
  upload:
    path: ./uploads                    # 文件存储路径
    url-prefix: /files                 # 文件访问URL前缀
    max-size: 10485760                 # 最大文件大小（字节），默认10MB
```

## 使用示例

### 1. 在业务服务中注入FileService

```java
@RestController
@RequestMapping("/api/pet")
public class PetController {

    @Autowired
    private FileService fileService;

    // ...
}
```

### 2. 上传文件

```java
@PostMapping("/upload-image")
public ApiResponse<FileService.FileInfo> uploadImage(
        @RequestParam("file") MultipartFile file) {

    try {
        // 1. 验证文件
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "文件不能为空");
        }

        // 2. 检查文件类型（可选）
        if (!FileUtils.isImage(file.getOriginalFilename())) {
            return ApiResponse.error(400, "只支持图片文件");
        }

        // 3. 转换为InputStream并上传
        InputStream inputStream = FileUtils.toInputStream(file);
        FileService.FileInfo fileInfo = fileService.uploadFile(
            inputStream,
            file.getOriginalFilename(),
            "pet"  // 文件分类：pet/org/user等
        );

        // 4. 将relativePath存储到数据库
        // pet.setImageUrl(fileInfo.getRelativePath());
        // petMapper.update(pet);

        return ApiResponse.success(fileInfo);

    } catch (Exception e) {
        return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
    }
}
```

### 3. 存储文件信息到数据库

业务模块的数据库表应包含文件路径字段，例如：

```sql
CREATE TABLE pet (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    image_url VARCHAR(500),  -- 存储FileInfo.getRelativePath()
    video_url VARCHAR(500),   -- 存储FileInfo.getRelativePath()
    -- 其他字段...
);
```

### 4. 前端展示文件

前端使用 `FileInfo.getUrl()` 来展示文件：

```javascript
// 上传后返回的fileInfo
{
  "relativePath": "pet/2024-01-15/uuid.jpg",
  "url": "/files/pet/2024-01-15/uuid.jpg",
  "originalFilename": "pet-photo.jpg",
  "size": 102400,
  "contentType": "image/jpeg"
}

// 前端展示
<img src={fileInfo.url} alt="宠物图片" />
```

### 5. 删除文件

```java
@DeleteMapping("/pet/{id}")
public ApiResponse<String> deletePet(@PathVariable Long id) {
    Pet pet = petMapper.findById(id);

    // 删除文件
    if (pet.getImageUrl() != null) {
        fileService.deleteFile(pet.getImageUrl());
    }

    // 删除数据库记录
    petMapper.delete(id);

    return ApiResponse.success("删除成功");
}
```

### 6. 文件下载

```java
@GetMapping("/file/{relativePath}")
public ResponseEntity<Resource> downloadFile(@PathVariable String relativePath) {
    try {
        InputStream inputStream = fileService.readFile(relativePath);
        FileService.FileInfo fileInfo = // 从数据库查询或根据路径构建

        ByteArrayResource resource = new ByteArrayResource(
            inputStream.readAllBytes()
        );

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                   "attachment; filename=\"" + fileInfo.getOriginalFilename() + "\"")
            .contentType(MediaType.parseMediaType(fileInfo.getContentType()))
            .body(resource);

    } catch (Exception e) {
        return ResponseEntity.notFound().build();
    }
}
```

## 文件存储结构

文件按以下结构存储：

```
uploads/
  ├── pet/              # 宠物相关文件
  │   ├── 2024-01-15/
  │   │   ├── uuid1.jpg
  │   │   └── uuid2.mp4
  │   └── 2024-01-16/
  │       └── uuid3.jpg
  ├── org/              # 机构相关文件
  │   └── 2024-01-15/
  │       └── uuid4.pdf
  └── user/             # 用户相关文件
      └── 2024-01-15/
          └── uuid5.jpg
```

## 注意事项

1. **文件路径存储**：业务模块应存储 `FileInfo.getRelativePath()`，而不是完整路径
2. **文件分类**：使用有意义的分类名称（如：pet, org, user），便于管理
3. **文件大小限制**：默认10MB，可在配置文件中修改
4. **文件删除**：删除业务记录时，记得同时删除关联的文件
5. **文件访问**：需要配置静态资源映射，将 `/files/**` 映射到文件存储目录

## 静态资源映射配置

在Gateway或各个服务中配置静态资源映射（示例）：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
```

