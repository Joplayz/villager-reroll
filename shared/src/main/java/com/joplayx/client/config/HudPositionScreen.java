package com.joplayx.client.config;

import com.joplayx.client.VillagerRerollClient;
import com.mojang.blaze3d.platform.InputConstants;
import com.joplayx.client.hud.RerollerHud;
import com.joplayx.client.util.Text;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

/** Position, size and snapping are drafts until Done is clicked. */
public class HudPositionScreen extends Screen {
    private final Screen parent;
    private double px, py, scale, grabX, grabY;
    private boolean dragging, snap;
    private Button smaller, larger, size;
    public HudPositionScreen(Screen parent) {
        super(Text.tr("hud.move"));this.parent=parent;
        var cfg=RerollerConfig.get();px=cfg.hudX;py=cfg.hudY;scale=RerollerHud.validScale(cfg.hudScale);snap=cfg.hudSnap;
    }
    @Override protected void init() {
        dragging=false;VillagerRerollClient.CONTROLLER.pauseForConfig(minecraft);
        int count=RerollerConfig.get().hudDetails?4:3;
        int w=Math.min(100,(width-24)/count),left=(width-count*w-(count-1)*4)/2;
        addRenderableWidget(Button.builder(Text.tr("button.done"),b->{
            var cfg=RerollerConfig.get();cfg.hudX=px;cfg.hudY=py;cfg.hudScale=scale;cfg.hudSnap=snap;RerollerConfig.save();onClose();
        }).bounds(left,height-28,w,20).build());
        addRenderableWidget(Button.builder(Text.tr("hud.reset_position"),b->{px=0;py=0;}).bounds(left+w+4,height-28,w,20).build());
        addRenderableWidget(Button.builder(Text.tr("button.cancel"),b->onClose()).bounds(left+2*w+8,height-28,w,20).build());
        if(RerollerConfig.get().hudDetails)addRenderableWidget(Button.builder(Text.tr("hud.next_page"),b->RerollerHud.nextPage()).bounds(left+3*w+12,height-28,w,20).build());
        int controlWidth=Math.min(100,(width-24)/4),controlLeft=(width-4*controlWidth-12)/2;
        smaller=addRenderableWidget(Button.builder(Text.tr("hud.smaller"),b->changeScale(-0.05)).bounds(controlLeft,height-52,controlWidth,20).build());
        size=addRenderableWidget(Button.builder(Text.tr("hud.scale",Math.round(scale*100)),b->{scale=1;updateSize();}).bounds(controlLeft+controlWidth+4,height-52,controlWidth,20).build());
        larger=addRenderableWidget(Button.builder(Text.tr("hud.larger"),b->changeScale(0.05)).bounds(controlLeft+2*controlWidth+8,height-52,controlWidth,20).build());
        addRenderableWidget(Button.builder(Text.tr(snap?"hud.snap_on":"hud.snap_off"),b->{snap=!snap;b.setMessage(Text.tr(snap?"hud.snap_on":"hud.snap_off"));}).bounds(controlLeft+3*controlWidth+12,height-52,controlWidth,20).build());
        updateSize();
    }
    private void changeScale(double delta){scale=RerollerHud.validScale(Math.round((scale+delta)*100)/100.0);updateSize();}
    private void updateSize(){size.setMessage(Text.tr("hud.scale",Math.round(scale*100)));smaller.active=scale>0.5;larger.active=scale<1.5;}
    private int canvasHeight(){return Math.max(120,height-76);}
    private int panelHeight(){return RerollerHud.panelHeight(width,canvasHeight(),scale);}
    private int panelWidth(){return RerollerHud.panelWidth(width,canvasHeight(),scale);}
    /** Snap within eight GUI pixels of either edge or the center; otherwise drag freely. */
    public static double alignedFraction(double value,int travel,boolean snap){
        value=Math.clamp(value,0,1);
        if(snap&&travel>0){double nearest=Math.round(value*2)/2.0;if(Math.abs(value-nearest)*travel<=8)return nearest;}
        return value;
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(super.mouseClicked(event,doubleClick))return true;
        int x=RerollerHud.position(px,width,panelWidth()),y=RerollerHud.position(py,canvasHeight(),panelHeight());
        if(event.button()==InputConstants.MOUSE_BUTTON_LEFT&&event.x()>=x&&event.x()<x+panelWidth()&&event.y()>=y&&event.y()<y+panelHeight()){
            dragging=true;grabX=event.x()-x;grabY=event.y()-y;return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        if(!dragging||event.button()!=InputConstants.MOUSE_BUTTON_LEFT)return super.mouseDragged(event,dx,dy);
        int travelX=Math.max(1,width-panelWidth()-12),travelY=Math.max(1,canvasHeight()-panelHeight()-12);
        px=alignedFraction((event.x()-grabX-6)/travelX,travelX,snap);py=alignedFraction((event.y()-grabY-6)/travelY,travelY,snap);return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event){
        if(dragging&&event.button()==InputConstants.MOUSE_BUTTON_LEFT){dragging=false;return true;}return super.mouseReleased(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.fill(0,0,width,height,0x88101418);
        if(snap){
            for(int x:new int[]{6,width/2,width-6})g.fill(x,6,x+1,canvasHeight()-6,0x669DBBCB);
            for(int y:new int[]{6,canvasHeight()/2,canvasHeight()-6})g.fill(6,y,width-6,y+1,0x669DBBCB);
        }
        RerollerHud.drawPanel(g,RerollerHud.lines(width,canvasHeight(),true,scale),px,py,0xFF9DBBCB,canvasHeight(),scale);
        g.text(font,font.plainSubstrByWidth(Text.str(snap?"hud.snap_hint":"hud.drag_hint"),width-16),8,height-68,0xFFFFFFFF);
        for(var child:children())if(child instanceof Button b)b.extractRenderState(g,mx,my,dt);
    }
}
