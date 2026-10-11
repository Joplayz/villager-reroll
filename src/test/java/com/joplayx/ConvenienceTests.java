package com.joplayx;
import com.joplayx.client.config.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
public class ConvenienceTests {
    @Test public void wishlistRoundTripExcludesResultsAndDoesNotShareTargets(){
        var t=new RerollerConfig.EnchantTarget("minecraft:mending",1,15);t.record(12,1,1,1,"saved");
        var p=WishlistStore.capture("My books","lectern",List.of(t));
        String json=WishlistStore.export(p);
        var copy=WishlistStore.parse(json);
        assertEquals("My books",copy.name);assertEquals(15,copy.targets.getFirst().maxEmeraldCost);
        assertEquals(-1,copy.targets.getFirst().bestPrice);assertTrue(copy.targets.getFirst().active());
        copy.targets.getFirst().maxEmeraldCost=1;assertEquals(15,t.maxEmeraldCost);
        assertFalse(json.contains("saved"));
    }
    @Test public void importedItemKeepsBothPayments(){
        var t=new RerollerConfig.EnchantTarget();t.kind=SearchTarget.Kind.ITEM;t.itemId="minecraft:flint";
        t.paymentItemId="minecraft:gravel";t.secondPaymentItemId="minecraft:emerald";t.maxSecondPayment=1;t.minQuantity=10;
        var p=WishlistStore.parse(WishlistStore.export(WishlistStore.capture("Flint","fletching_table",List.of(t))));
        assertEquals("minecraft:gravel",p.targets.getFirst().paymentItemId);assertEquals(10,p.targets.getFirst().minQuantity);
        assertEquals(1,p.targets.getFirst().maxSecondPayment);
    }
    @Test public void invalidImportsAreRejected(){
        for(String invalid:List.of("null","{broken","{}","{\"format\":2}"," ".repeat(65537),
                "{\"name\":\"Bad\",\"targets\":[null]}")){
            try{WishlistStore.parse(invalid);fail("Accepted invalid wishlist");}catch(IllegalArgumentException expected){}
        }
    }
    @Test public void compareWithPreviousBest(){
        var t=new SearchTarget();t.record(21,1,3,1,"first");t.record(11,1,3,1,"second");
        assertEquals(21,t.previousBestPrice);assertEquals(11,t.bestPrice);
        t.reset();assertEquals(-1,t.previousBestPrice);
    }
    @Test public void starterPresetsAreIndependent(){
        var p=WishlistStore.starters();assertEquals(3,p.size());
        p.getFirst().targets.clear();assertFalse(WishlistStore.starters().getFirst().targets.isEmpty());
    }
}
