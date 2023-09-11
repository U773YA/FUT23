package org.example.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.AttributeValues;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CardUtil {

    public static Map<String, AttributeValues> populateCardAttributes(String cardJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, List<AttributeValues>>> list = mapper.readValue(cardJson, new TypeReference<>() {});

        List<AttributeValues> attributeValuesList =  new HashMap<>(list.get(0)).values().stream().flatMap(List::stream).toList();
        return attributeValuesList.stream().collect(Collectors.toMap(AttributeValues::getId, Function.identity(), (value1, value2) -> value1));
    }
}
