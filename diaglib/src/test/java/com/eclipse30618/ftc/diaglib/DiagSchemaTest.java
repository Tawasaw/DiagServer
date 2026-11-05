package com.eclipse30618.ftc.diaglib;

import org.junit.Test;
import static org.junit.Assert.*;

public class DiagSchemaTest {
    private DiagSchema parse(String json) { return DiagSchema.fromConfig(null, json); }

    @Test public void resolvesForwardReferencesAndLoggingFields() {
        DiagSchema s = parse("{groups:{heading:{fields:{yaw:{ref:true}}},imu:{fields:{yaw:{unit:'deg'}}},metadata:{graph:false,fields:{run_id:{type:'long'}}}}}");
        assertEquals(2, s.getFields().size());
        assertEquals("run_id", s.getFields().get(0).name);
        assertEquals("long", s.getFields().get(0).type);
        assertEquals("double", s.getFields().get(1).type);
        assertEquals("deg", s.getFields().get(1).unit);
        FieldCatalog catalog = new FieldCatalog();
        for (SchemaField f : s.getFields()) catalog.add(f.name, f.type, f.unit);
        DiagSnapshotBuilder builder = new DiagSnapshotBuilder(2);
        builder.set(s.indexOf("run_id"), "123");
        builder.set(s.indexOf("yaw"), "45");
        assertEquals("45,123", builder.build().toCsv(new int[] {catalog.indexOf("yaw"), catalog.indexOf("run_id")}));
    }

    @Test public void rejectsInvalidSchemas() {
        String[] invalid = {
            "{fields:[]}", "{graphs:[]}", "{subscribe:[]}", "{version:2}",
            "{groups:{g:{fields:{x:{ref:true}}}}}",
            "{groups:{g:{fields:{x:{}}},h:{fields:{x:{}}}}}",
            "{groups:{g:{fields:{x:{type:'float'}}}}}",
            "{groups:{g:{fields:{x:{ref:true,unit:'deg'}}}}}",
            "{groups:{g:{graph:'false',fields:{x:{}}}}}"
        };
        for (String json : invalid) {
            try { parse(json); fail("Accepted: " + json); }
            catch (IllegalArgumentException expected) { }
        }
    }
}
