package com.boilerplate.spring_boot.convertors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.sql.SQLException;

@WritingConverter
@Slf4j
public class JsonArrayToPGobject implements Converter<JsonArray, PGobject> {
    @Override
    public PGobject convert(JsonArray source) {
        PGobject object = new PGobject();
        Gson gson = new Gson();
        try {
            object.setValue(gson.toJson(source));
        } catch (SQLException e) {
            log.error("Error saving JsonArray: {} due to {}", source, e);
            return null;
        }
        object.setType("jsonb");
        return object;
    }
}
