package com.netgrif.mongo.quartz;


import com.netgrif.quartz.mongodb.Constants;
import com.netgrif.quartz.mongodb.JobDataConverter;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobPersistenceException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class JobDataConverterTest {

    private JobDataConverter converterBase64;
    private JobDataConverter converterPlain;
    private String base64 = "rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAACdAADc3RydAADMTIzdAADZm9vc3IAMWNvbS5uZXRncmlmLm1vbmdvLnF1YXJ0ei5Kb2JEYXRhQ29udmVydGVyVGVzdCRGb29jnxlZgcJi3AIAAkwAA2JhcnQAM0xjb20vbmV0Z3JpZi9tb25nby9xdWFydHovSm9iRGF0YUNvbnZlcnRlclRlc3QkQmFyO0wAA3N0cnQAEkxqYXZhL2xhbmcvU3RyaW5nO3hwc3IAMWNvbS5uZXRncmlmLm1vbmdvLnF1YXJ0ei5Kb2JEYXRhQ29udmVydGVyVGVzdCRCYXIeonWPEVlgfwIAAUwAA3N0cnEAfgAHeHB0AANhYmN0AANkZWZ4";

    @BeforeEach
    public void setUp() {
        converterBase64 = new JobDataConverter(true);
        converterPlain = new JobDataConverter(false);
    }

    @Test
    public void emptyJobDataDoesNotModifyTheDocument() throws JobPersistenceException {
        JobDataMap emptyJobDataMap = new JobDataMap();
        Document doc = new Document();

        converterBase64.toDocument(emptyJobDataMap, doc);
        assertEquals(0, doc.size());

        converterPlain.toDocument(emptyJobDataMap, doc);
        assertEquals(0, doc.size());
    }

    @Test
    public void documentWithoutJobDataDoesNotModifyJobDataMap() throws JobPersistenceException {
        Document doc = new Document();
        doc.put("foo", "bar");
        doc.put("num", 123);
        JobDataMap jobDataMap = new JobDataMap();

        assertFalse(converterBase64.toJobData(doc, jobDataMap));
        assertEquals(0, jobDataMap.size());

        assertFalse(converterPlain.toJobData(doc, jobDataMap));
        assertEquals(0, jobDataMap.size());
    }

    @Test
    public void testBase64EncodeWorks() throws JobPersistenceException {
        JobDataMap jobDataMap = createJobDataWithSerializableContent();
        Document doc = new Document();

        converterBase64.toDocument(jobDataMap, doc);
        assertEquals(1, doc.size());
        assertEquals(base64, doc.get(Constants.JOB_DATA));
    }

    @Test
    public void testBase64DecodeWorks() throws JobPersistenceException {
        Document doc = new Document();
        doc.put(Constants.JOB_DATA, base64);
        JobDataMap jobDataMap = new JobDataMap();

        assertTrue(converterBase64.toJobData(doc, jobDataMap));
        assertEquals(2, jobDataMap.getWrappedMap().size());
        assertMapEquals(createJobDataWithSerializableContent().getWrappedMap(), jobDataMap.getWrappedMap());
    }

    @Test
    public void testBase64DecodeFails() {
        Document doc = new Document();
        doc.put(Constants.JOB_DATA, 'a' + base64);
        JobDataMap jobDataMap = new JobDataMap();

        assertThrows(JobPersistenceException.class, () -> {
            converterBase64.toJobData(doc, jobDataMap);
        });
    }

    @Test
    public void testPlainEncodeWorks() throws JobPersistenceException {
        JobDataMap jobDataMap = createJobDataWithSimpleContent();
        Document doc = new Document();

        converterPlain.toDocument(jobDataMap, doc);
        assertEquals(1, doc.size());
        assertMapEquals(createJobDataWithSimpleContent().getWrappedMap(), (Map)doc.get(Constants.JOB_DATA_PLAIN));
    }

    @Test
    public void testPlainDecodeWorks() throws JobPersistenceException {
        Document doc = new Document();
        doc.put(Constants.JOB_DATA_PLAIN, createJobDataWithSimpleContent().getWrappedMap());
        JobDataMap jobDataMap = new JobDataMap();

        assertTrue(converterPlain.toJobData(doc, jobDataMap));
        assertEquals(2, jobDataMap.getWrappedMap().size());

        Map<String, Object> expectedMap = createJobDataWithSimpleContent().getWrappedMap();
        Map<String, Object> actualMap = jobDataMap.getWrappedMap();

        for (String key : expectedMap.keySet()) {
            Object expectedValue = expectedMap.get(key);
            Object actualValue = actualMap.get(key);

            if (expectedValue instanceof Map) {
                assertMapEquals((Map<?, ?>) expectedValue, (Map<?, ?>) actualValue);
            } else if (expectedValue instanceof List) {
                assertListsEqual((List<?>) expectedValue, (List<?>) actualValue);
            } else {
                assertEquals(expectedValue, actualValue);
            }
        }
    }



    @Test
    public void testPlainDecodeFallsBackToBase64() throws JobPersistenceException {
        Document doc = new Document();
        doc.put(Constants.JOB_DATA, base64);
        JobDataMap jobDataMap = new JobDataMap();

        assertTrue(converterPlain.toJobData(doc, jobDataMap));
        assertEquals(2, jobDataMap.getWrappedMap().size());
        assertMapEquals(createJobDataWithSerializableContent().getWrappedMap(), jobDataMap.getWrappedMap());
    }

    private JobDataMap createJobDataWithSerializableContent() {
        Foo foo = new Foo(new Bar("abc"), "def");
        Map<String, Object> map = new HashMap<>();
        map.put("foo", foo);
        map.put("str", "123");
        return new JobDataMap(map);
    }

    private JobDataMap createJobDataWithSimpleContent() {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "foo");
        Map<String, Object> barMap = new HashMap<>();
        barMap.put("one", 1);
        barMap.put("two", 2.0);
        barMap.put("list", new String[]{"a", "b", "c"});
        map.put("bar", barMap);
        return new JobDataMap(map);
    }

    @ToString
    @EqualsAndHashCode
    static class Foo implements Serializable {
        Bar bar;
        String str;

        public Foo(Bar bar, String str) {
            this.bar = bar;
            this.str = str;
        }
    }

    @ToString
    @EqualsAndHashCode
    static class Bar implements Serializable {
        String str;

        public Bar(String str) {
            this.str = str;
        }
    }

    private void assertMapEquals(Map<?, ?> expectedMap, Map<?, ?> actualMap) {
        assertEquals(expectedMap.size(), actualMap.size());
        for (Object key : expectedMap.keySet()) {
            assertTrue(actualMap.containsKey(key));
            Object expectedValue = expectedMap.get(key);
            Object actualValue = actualMap.get(key);

            if (expectedValue instanceof Object[] && actualValue instanceof Object[]) {
                assertArrayEquals((Object[]) expectedValue, (Object[]) actualValue);
            } else {
                areEqualKeyValues(expectedMap, actualMap);
            }
        }
    }

    private Map<?, Boolean> areEqualKeyValues(Map<?, ?> first, Map<?, ?> second) {
        return first.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(),
                        e -> e.getValue().equals(second.get(e.getKey()))));
    }


    private void assertListsEqual(List<?> expectedList, List<?> actualList) {
        assertEquals(expectedList.size(), actualList.size());
        for (int i = 0; i < expectedList.size(); i++) {
            assertEquals(expectedList.get(i), actualList.get(i));
        }
    }
}
