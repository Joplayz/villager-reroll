package com.joplayx.client.util;

import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.config.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.*;
import java.util.*;

public final class QuickSetup {
    public record Candidate(BlockPos pos,Workstation station) {}
    private static UUID selected;
    private static ClientLevel selectedWorld;
    private static int entityId=-1, highlightTicks;
    private static BlockPos highlightPos;
    public static boolean hasSelection() { return selected!=null; }
    public static Villager selected(Minecraft mc) {
        if(mc.level==null||mc.level!=selectedWorld) return null;
        var e=mc.level.getEntity(entityId);
        return e instanceof Villager v&&v.isAlive()&&v.getUUID().equals(selected)?v:null;
    }
    public static void clear() { VillagerRerollClient.CONTROLLER.clearVillagerSelection();previewVillager=null; selected=null;selectedWorld=null;entityId=-1;highlightPos=null;highlightTicks=0; }
    public static String profession(Villager v) {
        return v.getVillagerData().profession().unwrapKey().map(k->k.identifier().getPath()).orElse("");
    }
    public static List<Candidate> candidates(Minecraft mc,Villager v) {
        if(mc.level==null) return List.of();
        var result=new ArrayList<Candidate>();String profession=profession(v);
        for(var pos:BlockPos.betweenClosed(v.blockPosition().offset(-4,-2,-4),v.blockPosition().offset(4,2,4))) {
            for(var station:Workstation.values()) if((profession.equals("none")||profession.equals(station.profession))
                    &&mc.level.getBlockState(pos).is(station.block)) result.add(new Candidate(pos.immutable(),station));
        }
        result.sort(Comparator.comparingDouble(c->c.pos().distToCenterSqr(v.position())));
        return result;
    }
    public static void fromCrosshair(Minecraft mc) {
        if(mc.player==null||mc.level==null) return;
        VillagerRerollClient.CONTROLLER.pauseForConfig(mc);
        if(mc.hitResult instanceof EntityHitResult hit&&hit.getEntity() instanceof Villager v) {
            if(v.isBaby()||v.getVillagerData().level()!=1||profession(v).equals("nitwit")) {
                mc.player.sendSystemMessage(Text.tr("setup.ineligible"));return;
            }
            mc.setScreen(new SetupScreen(null,v,candidates(mc,v)));return;
        }
        if(mc.hitResult instanceof BlockHitResult hit) {
            for(var s:Workstation.values()) if(mc.level.getBlockState(hit.getBlockPos()).is(s.block)) {
                clear(); var cfg=RerollerConfig.get(); cfg.workstation=s.id;cfg.setLecternPos(hit.getBlockPos());
                RerollerConfig.save();preview(null,hit.getBlockPos());
                mc.player.sendSystemMessage(Text.tr("setup.block",s.block.getName()));return;
            }
        }
        mc.player.sendSystemMessage(Text.tr("setup.aim"));
    }
    public static boolean confirm(Minecraft mc,Villager v,Candidate c) {
        if(mc.level==null||mc.player==null||mc.level.getEntity(v.getId())!=v||!v.isAlive()
                ||v.isBaby()||v.getVillagerData().level()!=1||!mc.level.getBlockState(c.pos()).is(c.station().block)) return false;
        var profession=profession(v);
        if(!profession.equals("none")&&!profession.equals(c.station().profession)) return false;
        selected=v.getUUID();entityId=v.getId();selectedWorld=mc.level;
        var cfg=RerollerConfig.get();cfg.workstation=c.station().id;cfg.setLecternPos(c.pos());
        RerollerConfig.save();preview(v,c.pos());return true;
    }
    public static void preview(Villager v,BlockPos pos) {
        highlightPos=pos;highlightTicks=160;
        previewVillager=v;
    }
    private static Villager previewVillager;
    public static void tick(Minecraft mc) {
        if(mc.player==null||mc.level==null) { clear();previewVillager=null;return; }
        if(selectedWorld!=null&&selectedWorld!=mc.level) clear();
        if(highlightTicks<=0) return;
        if(highlightTicks--%5!=0) return;
        if(highlightPos!=null) for(int i=0;i<4;i++) {
            double x=highlightPos.getX()+((i&1)==0?.05:.95),z=highlightPos.getZ()+((i&2)==0?.05:.95);
            mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER,x,highlightPos.getY()+1.1,z,0,0,0);
        }
        if(previewVillager!=null&&previewVillager.isAlive()&&mc.level.getEntity(previewVillager.getId())==previewVillager)
            mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER,previewVillager.getX(),previewVillager.getY()+2.1,previewVillager.getZ(),0,0,0);
    }
}
