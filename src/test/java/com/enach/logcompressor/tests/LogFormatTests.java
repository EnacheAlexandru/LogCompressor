package com.enach.logcompressor.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class LogFormatTests {

    @Value("${logcompressor.compressed.log.filename}")
    private String COMPRESSED_LOG_FILENAME;

    @Autowired
    private MockMvc mockMvc;

    private final String MAIN_PATH = "src/main/resources/";

    private final String LOG_PATH = MAIN_PATH + "testlogs/";

    @Test
    public void testCompressHdfs() throws Exception {
        testByLogFormat("hdfs_100L.log");
    }

    @Test
    public void testCompressAndroid() throws Exception {
        testByLogFormat("android_100L.log");
    }

    @Test
    public void testCompressMac() throws Exception {
        testByLogFormat("mac_100L.log");
    }

    @Test
    public void testCompressWindows() throws Exception {
        testByLogFormat("windows_100L.log");
    }

    @Test
    public void testCompressZookeeper() throws Exception {
        testByLogFormat("zookeeper_100L.log");
    }

    private void testByLogFormat(String filePath) throws Exception {

        // COMPRESS
        Path path = Paths.get(LOG_PATH + filePath);
        byte[] originalContent = Files.readAllBytes(path);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                filePath,
                MediaType.MULTIPART_FORM_DATA_VALUE,
                originalContent
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/compress").file(file))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // DECOMPRESS
        filePath = COMPRESSED_LOG_FILENAME;
        path = Paths.get(MAIN_PATH + filePath);
        byte[] compressedContent = Files.readAllBytes(path);

        file = new MockMultipartFile(
                "file",
                filePath,
                MediaType.MULTIPART_FORM_DATA_VALUE,
                compressedContent
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/decompress").file(file))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // DOWNLOAD DECOMPRESSED FILE
        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.get("/decompress/download"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        byte[] decompressedContent = response.getResponse().getContentAsByteArray();

        // ASSERT
        Assertions.assertArrayEquals(originalContent, decompressedContent);
    }
}
