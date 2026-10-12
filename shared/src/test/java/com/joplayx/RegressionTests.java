package com.joplayx;
import com.joplayx.client.config.SearchTarget;
import com.joplayx.client.state.SessionStats;
import com.joplayx.client.util.TradeMatcher;
import com.google.gson.Gson;
import java.util.Map;
public class RegressionTests {
    private static int assertions;
    private static void check(boolean condition,String reason){assertions++;if(!condition)throw new AssertionError(reason);}
    private static TradeMatcher.Offer book(int price,int level){return new TradeMatcher.Offer("minecraft:enchanted_book",1,"minecraft:emerald",price,"minecraft:book",1,Map.of("minecraft:thorns",level));}
    @org.junit.Test public void regressionChecks() {
        SearchTarget t=new Gson().fromJson("{\"enchantmentId\":\"minecraft:thorns\",\"minLevel\":3,\"maxEmeraldCost\":21}",SearchTarget.class);
        check(t.active()&&t.kind==SearchTarget.Kind.ENCHANTMENT,"legacy defaults");
        check(TradeMatcher.matches(t,book(21,3)),"inclusive price and level boundaries");
        check(!TradeMatcher.matches(t,book(22,3)),"over budget");
        check(!TradeMatcher.matches(t,book(10,2)),"under level");
        t.record(21,1,3,1,"thorns III");
        check(!TradeMatcher.matches(t,book(15,3)),"found requires explicit continue");
        t.status=SearchTarget.Status.ACTIVE;
        check(!TradeMatcher.matches(t,book(21,3)),"equal price must not notify again");
        check(TradeMatcher.matches(t,book(15,3)),"improved price notifies");
        t.record(15,1,3,1,"better"); t.status=SearchTarget.Status.ACTIVE;
        check(!TradeMatcher.matches(t,book(18,3)),"worse price ignored");
        check(TradeMatcher.matches(t,book(11,3)),"second improvement notifies");
        t.status=SearchTarget.Status.ACQUIRED; check(!TradeMatcher.matches(t,book(1,3)),"acquired suppressed");
        t.status=SearchTarget.Status.IGNORED; check(!TradeMatcher.matches(t,book(1,3)),"ignored suppressed");
        t.reset(); check(t.active()&&t.bestPrice==-1&&t.enchantmentId.equals("minecraft:thorns")&&t.maxEmeraldCost==21,"reset keeps criteria");
        t.kind=SearchTarget.Kind.ITEM;t.itemId="minecraft:emerald";t.paymentItemId="minecraft:wheat";t.maxEmeraldCost=20;
        var wheat=new TradeMatcher.Offer("minecraft:emerald",1,"minecraft:wheat",20,"",0,Map.of());
        check(TradeMatcher.matches(t,wheat),"villager buying wheat");
        t.minQuantity=2;check(!TradeMatcher.matches(t,wheat),"quantity filter");t.minQuantity=1;
        check(!TradeMatcher.matches(t,new TradeMatcher.Offer("minecraft:emerald",1,"minecraft:carrot",20,"",0,Map.of())),"payment type matters");
        var second=new TradeMatcher.Offer("minecraft:emerald",1,"minecraft:wheat",20,"minecraft:book",2,Map.of());
        check(!TradeMatcher.matches(t,second),"unexpected second cost rejected");
        t.secondPaymentItemId="minecraft:book";t.maxSecondPayment=2;
        check(TradeMatcher.matches(t,second),"explicit second cost accepted");
        t.maxSecondPayment=1;check(!TradeMatcher.matches(t,second),"second cost cap");
        t.record(20,1,0,0,"saved");t.status=SearchTarget.Status.ACTIVE;
        check(t.better(20,2,0,0),"better unit price");check(!t.better(10,1,0,1),"secondary cost increase isn't improvement");
        var copy=new Gson().fromJson(new Gson().toJson(t),SearchTarget.class);
        check(copy.bestPrice==20&&copy.status==SearchTarget.Status.ACTIVE&&copy.paymentItemId.equals("minecraft:wheat"),"save/load retains result and status");
        var stats=new SessionStats();for(int i=0;i<60;i++)stats.tick();stats.attempt();stats.attempt();
        check(stats.attempts()==2&&stats.seconds()==3,"session stats before interruption");
        stats.tick();stats.attempt();check(stats.attempts()==3&&stats.seconds()==3,"resume preserves stats");
        stats.reset();check(stats.attempts()==0&&stats.seconds()==0,"explicit new session resets");
        System.out.println("Passed "+assertions+" regression assertions.");
    }
}
