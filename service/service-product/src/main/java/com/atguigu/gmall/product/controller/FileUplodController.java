package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * @author atguigu-mqx
 */
@RestController
@RequestMapping("admin/product/")
public class FileUplodController {

    //  获取到文件服务器的地址，账号，密码
    /*
    minio:
      endpointUrl: http://39.99.159.121:9000
      accessKey: admin
      secreKey: admin123456
      bucketName: gmall
     */
    @Value("${minio.endpointUrl}")
    private String endpointUrl;
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secreKey}")
    private String secreKey;
    @Value("${minio.bucketName}")
    private String bucketName;



    //  http://localhost/admin/product/fileUpload
    //  参数file ：springmvc 文件上传类
    @PostMapping("fileUpload")
    public Result fileUplod(MultipartFile file) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        String url= "";
        try {
            // 使用MinIO服务的URL，端口，Access key和Secret key创建一个MinioClient对象
            //  MinioClient minioClient = new MinioClient("https://play.min.io", "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(endpointUrl)
                            .credentials(accessKey, secreKey)
                            .build();
            // 检查存储桶是否已经存在
            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if(isExist) {
                System.out.println("Bucket already exists.");
            } else {
                // 创建一个名为asiatrip的存储桶，用于存储照片的zip文件。
                //  minioClient.makeBucket("asiatrip");
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build());
            }

            //  上传文件的时候，需要注意的是：文件名称不能重复，文件的后缀名要与原来的后缀名保持一致！
            //  1232.png fileName.png
            String extName = FilenameUtils.getExtension(file.getOriginalFilename());    // png
            //  1623051196858d86543a89170472384e5f3a5e781ba84.png
            String fileName = System.currentTimeMillis()+ UUID.randomUUID().toString().replace("-","")+"."+extName;
            // 使用putObject上传一个文件到存储桶中。
            //  minioClient.putObject("asiatrip","asiaphotos.zip", "/home/user/Photos/asiaphotos.zip");
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                            file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            //  获取到上传之后的URL：
            url =
                    minioClient.getPresignedObjectUrl(
                            GetPresignedObjectUrlArgs.builder()
                                    .method(Method.GET)
                                    .bucket(bucketName)
                                    .object(fileName)
                                    .build());
            System.out.println(url);

        } catch(MinioException e) {
            System.out.println("Error occurred: " + e);
        }
        //  返回文件上传之后的url！
        return Result.ok(url);
    }

}
