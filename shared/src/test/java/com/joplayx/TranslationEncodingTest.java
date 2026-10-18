package com.joplayx;
import com.google.gson.JsonParser;
import org.junit.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
public class TranslationEncodingTest {
    @Test public void englishTradeMessageUsesRealMultiplicationSymbols() throws Exception {
        try(var input=getClass().getResourceAsStream("/assets/villager-reroll/lang/en_us.json")) {
            assertNotNull(input);
            var translations=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("%s \u00d7 %s for %s \u00d7 %s%s",translations.get("villager-reroll.trade.item_description").getAsString());
            for(var entry:translations.entrySet())assertFalse("Misencoded translation: "+entry.getKey(),entry.getValue().getAsString().matches(".*[\u00c2\u00c3].*"));
        }
    }
}
