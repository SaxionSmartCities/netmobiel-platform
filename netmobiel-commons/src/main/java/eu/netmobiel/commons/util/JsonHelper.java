package eu.netmobiel.commons.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.json.*;
import javax.json.stream.JsonGenerator;

import eu.netmobiel.commons.exception.SystemException;

public class JsonHelper {
	private JsonHelper() {
		// Do not instantiate
	}
	
	public static String prettyPrint(JsonStructure json) {
        return jsonFormat(json, JsonGenerator.PRETTY_PRINTING);
    }

    public static String jsonFormat(JsonStructure json, String... options) {
        StringWriter stringWriter = new StringWriter();
        Map<String, Boolean> config = buildConfig(options);
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        try (JsonWriter jsonWriter = writerFactory.createWriter(stringWriter)) {
            jsonWriter.write(json);
        }
        return stringWriter.toString();
    }

    public static JsonObject parseJson(String jsonText) {
        JsonObject json = null;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonText))) {
            json = jsonReader.readObject();
        } catch (Exception e) {
            throw new SystemException(e);
        }
        return json;
    }

    private static Map<String, Boolean> buildConfig(String... options) {
        Map<String, Boolean> config = new HashMap<String, Boolean>();

        if (options != null) {
            for (String option : options) {
                config.put(option, true);
            }
        }

        return config;
    }
}
