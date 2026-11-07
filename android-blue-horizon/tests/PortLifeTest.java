package com.bluehorizon;
import java.nio.file.*;

public final class PortLifeTest {
    private static int count;
    private static void check(boolean value,String why) { count++;if(!value) throw new AssertionError(why); }
    public static void main(String[] args) throws Exception {
        ports();talks();guards();
        Engine old=Engine.decode(Files.readString(Paths.get("tests/fixtures/v8-export-save.txt")));
        check(old!=null && old.gold==1800 && old.life.events==0 && old.life.chats==0,"real BLUE8 migrates without invented choices");
        System.out.println("PASS: "+count+" port life checks");
    }
    private static void ports() {
        for(int port=0;port<56;port++) for(int choice=0;choice<2;choice++) {
            Engine e=new Engine();e.port=port;e.visited|=1L<<port;int gold=e.gold,day=e.day;
            check(PortLife.story(e).contains(World.PORTS[port]),"story names actual port");
            check(PortLife.choose(e,choice),"both local choices available");
            check(e.gold==gold+(choice==0?-40:90) && e.day==day+1,"stated local costs and rewards exact");
            check(!PortLife.choose(e,choice) && e.day==day+1,"local reward cannot repeat");
            Engine restored=Engine.decode(e.encode());check(restored!=null && PortLife.done(restored),"local choice survives reload");
        }
    }
    private static void guards() {
        Engine e=new Engine();e.gold=39;
        check(!PortLife.choose(e,0) && e.gold==39 && e.day==1 && e.life.events==0,"unaffordable choice does not consume event");
        e=new Engine();e.adventure.pending=0;check(!PortLife.choose(e,1),"pending sea event blocks local story");
        e=new Engine();check(!PortLife.talk(e,5,0),"unjoined companion cannot speak");
        e=new Engine();e.life.events=1L<<10;check(Engine.decode(e.encode())==null,"unvisited story flags rejected");
        e=new Engine();e.life.generous=1;check(Engine.decode(e.encode())==null,"choice flags require completed story");
    }
    private static void talks() {
        for(int region=0;region<7;region++) for(int person=1;person<=5;person++) for(int choice=0;choice<2;choice++) {
            Engine e=new Engine();e.chapter=6;e.port=region*8;e.visited|=1L<<e.port;
            check(PortLife.talk(e,person,choice),"regional companion conversation available");
            check(!PortLife.talk(e,person,1),"conversation cannot be farmed");
            check(Engine.decode(e.encode())!=null && PortLife.talked(Engine.decode(e.encode()),person),"conversation survives reload");
            check(PortLife.recorded(Engine.decode(e.encode()),person)==(choice==1),"remembered reply matches original choice");
        }
    }
}
