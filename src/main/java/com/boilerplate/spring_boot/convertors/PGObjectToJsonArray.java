package com.boilerplate.spring_boot.convertors;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class PGObjectToJsonArray implements Converter<PGobject, JsonArray> {
    @Override
    public JsonArray convert(PGobject source) {
        return JsonParser.parseString(source.getValue()).getAsJsonArray();
    }
}
