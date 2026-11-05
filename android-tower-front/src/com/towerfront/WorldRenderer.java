package com.towerfront;
import android.content.Context;
import android.graphics.*;

public final class WorldRenderer {
    private final Paint p=new Paint(3);
    private final Bitmap terrain;
    private final BuildingPainter buildings=new BuildingPainter();
    public WorldRenderer(Context context) {
        int id=context.getResources().getIdentifier("battlefield","drawable",context.getPackageName());
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
        terrain=id==0?null:BitmapFactory.decodeResource(context.getResources(),id,options);
        p.setFilterBitmap(true);
    }
    public void terrain(Canvas c,RectF bounds) {
        p.setColor(0x2224394b);c.drawRoundRect(bounds.left,bounds.top+6,bounds.right,bounds.bottom+6,22,22,p);
        c.save();Path clip=new Path();clip.addRoundRect(bounds,22,22,Path.Direction.CW);c.clipPath(clip);
        p.setAlpha(255);
        if(terrain!=null) c.drawBitmap(terrain,null,bounds,p);
        else {p.setColor(0xffb2cc85);c.drawRect(bounds,p);}
        c.restore();
    }
    public void tower(Canvas c,Engine.Tower t,boolean selected,float pop,double time) {
        buildings.draw(c,t,BattleControls.x(t),BattleControls.y(t),selected,pop,time);
    }
    public void hero(Canvas c) {
        terrain(c,new RectF(8,8,392,209));
        Engine.Tower left=new Engine.Tower(0,0,1,32),right=new Engine.Tower(0,0,2,24);
        Engine.Tower center=new Engine.Tower(0,0,1,18);center.kind=Engine.FACTORY;right.kind=Engine.FORTRESS;
        buildings.draw(c,left,93,122,false,1.05f,0);
        buildings.draw(c,center,200,87,false,1.1f,0);
        buildings.draw(c,right,307,128,false,1.05f,0);
    }
    public void fleet(Canvas c,Engine e,Engine.Fleet f) {
        Engine.Tower a=e.towers.get(f.from),b=e.towers.get(f.to);
        float ax=BattleControls.x(a),ay=BattleControls.y(a),bx=BattleControls.x(b),by=BattleControls.y(b);
        p.setColor(0x33515133);p.setStrokeWidth(2);p.setPathEffect(new DashPathEffect(new float[]{3,5},0));
        c.drawLine(ax,ay,bx,by,p);p.setPathEffect(null);
        double angle=Math.atan2(by-ay,bx-ax);
        for(int i=0;i<SceneStyle.squadSize(f.units);i++) {
            float v=(float)Math.max(0,f.progress-(i/3)*.018);
            float side=(i%3-1)*6;
            float x=ax+(bx-ax)*v-(float)Math.sin(angle)*side;
            float y=ay+(by-ay)*v+(float)Math.cos(angle)*side;
            soldier(c,x,y,f.owner,e.elapsed+i*.13);
        }
        number(c,ax+(bx-ax)*(float)f.progress,ay+(by-ay)*(float)f.progress-15,f.units);
    }
    private void soldier(Canvas c,float x,float y,int owner,double time) {
        float bob=(float)Math.sin(time*16)*.8f;
        p.setColor(0x4433412a);c.drawOval(x-4,y+2,x+4,y+5,p);
        p.setColor(0xff314a4a);p.setStrokeWidth(1.6f);
        c.drawLine(x-1,y+1,x-2,y+4+bob,p);c.drawLine(x+1,y+1,x+2,y+4-bob,p);
        p.setColor(SceneStyle.team(owner));c.drawRoundRect(x-3,y-4+bob,x+3,y+2+bob,2,2,p);
        p.setColor(0xffe9c79d);c.drawCircle(x,y-5+bob,2.3f,p);
        p.setColor(SceneStyle.team(owner));c.drawArc(x-3,y-9+bob,x+3,y-3+bob,180,180,true,p);
    }
    private void number(Canvas c,float x,float y,int units) {
        p.setColor(SceneStyle.INK);c.drawRoundRect(x-12,y-8,x+12,y+7,5,5,p);
        p.setColor(Color.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(10);c.drawText(String.valueOf(units),x,y+3,p);
    }
}
