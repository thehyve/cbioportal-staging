package org.cbioportal.staging.etl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cbioportal.staging.app.FreemarkerConfig;
import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.services.LogMessageUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { LogMessageUtils.class, FreemarkerConfig.class})
public class TestFreemarker {

    @Autowired
    private LogMessageUtils utils;

    @Test
    public void test() throws ReporterException {
        Map<String,List<String>> m = new HashMap<>();
        List<String> files = new ArrayList<>();
        files.add("file1");
        files.add("file2");
        m.put("study1", files);

        utils.messageStudyFileNotFound("studyFileNotFound.ftl", m, 12);
    }

}