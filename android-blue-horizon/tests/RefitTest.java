package com.bluehorizon;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public final class RefitTest {
    private static int checks;
    private static void check(boolean value,String text) { checks++;if(!value) throw new AssertionError(text); }
    private static void slotsAndResources() {
        Engine e=new Engine();e.gold=20000;
        check(Refits.install(e,0) && Refits.install(e,2) && Refits.install(e,3),"three small-ship modules");
        check(e.refits.used()==3 && Refits.slots(e)==3,"small ship slot budget");
        int gold=e.gold;
        check(!Refits.install(e,1) && e.gold==gold,"cannot exceed slot budget or charge on failure");
        check(e.capacity()==33 && e.maxHull()==115 && e.hull==115,"hull and storage equipment effects");
        cargoAndRefund(e);
    }
    private static void cargoAndRefund(Engine e) {
        e.inventory[0]=33;
        check(!Refits.remove(e,3) && e.capacity()==33,"cannot shrink cargo hold below load");
        e.inventory[0]=25;int gold=e.gold;
        check(Refits.remove(e,3) && e.gold>gold && e.capacity()==25,"uninstall gives partial refund");
        e.adventure.pending=0;
        check(!Refits.install(e,1) && !Refits.remove(e,0),"cannot refit during encounter");
    }
    private static void speedAndCombat() {
        Engine e=new Engine();e.gold=30000;int days=e.travelDays(28);
        for(int i=0;i<3;i++) check(Refits.install(e,0),"upgrade sails");
        check(e.travelDays(28)<days,"sail tiers reduce long voyage duration");
        check(!Refits.install(e,0),"maximum module tier");
        check(e.upgrade() && e.refits.levels[0]==3 && Refits.slots(e)==5,"new hull retains equipment and grows slots");
        Engine armed=new Engine();armed.gold=30000;Refits.install(armed,1);
        Engine plain=new Engine();
        armed.adventure.pending=1;plain.adventure.pending=1;
        armed.adventure.battleHP=60;plain.adventure.battleHP=60;
        SeaBattle.choose(armed,0);SeaBattle.choose(plain,0);
        check(armed.adventure.battleHP<plain.adventure.battleHP,"cannon module increases actual damage");
        e=new Engine();e.gold=30000;int damage=e.travelDamage(2,0);
        Refits.install(e,2);
        check(e.travelDamage(2,0)<damage,"reinforced hull mitigates travel damage");
    }
    private static void saveCompatibility() throws Exception {
        Engine e=new Engine();e.gold=20000;Refits.install(e,3);
        Engine restored=Engine.decode(e.encode());
        check(restored!=null && restored.refits.levels[3]==1,"module levels reload");
        e.refits.levels[0]=3;e.refits.levels[1]=3;
        check(Engine.decode(e.encode())==null,"overslotted save rejected");
        e=new Engine();e.refits.levels[0]=4;
        check(Engine.decode(e.encode())==null,"out-of-range module tier rejected");
        String fixture=new String(Files.readAllBytes(Paths.get("tests/fixtures/v4-save.txt")),StandardCharsets.US_ASCII);
        restored=Engine.decode(fixture);
        check(restored!=null && restored.crew.first==2 && restored.crew.second==5,"old crew assignments retained");
        check(restored.adventure.battleHP==38 && restored.refits.used()==0,"old battle retained with empty modules");
    }
    public static void main(String[] args) throws Exception {
        slotsAndResources();speedAndCombat();saveCompatibility();System.out.println("PASS: "+checks+" ship refit checks");
    }
}
