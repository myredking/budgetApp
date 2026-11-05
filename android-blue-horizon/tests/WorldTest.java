package com.bluehorizon;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class WorldTest {
    private static int checks;
    private static void check(boolean condition,String label) {
        checks++; if(!condition) throw new AssertionError(label);
    }
    private static void geography() {
        check(World.COUNT==56,"56 real-world ports");
        check(World.REGIONS.length==7,"seven trading regions");
        for(int region=0;region<7;region++) {
            check(World.portsIn(region).length==8,"eight ports per region");
            MapProjection map=new MapProjection(region);
            checkLabels(map);
            for(int port:World.portsIn(region)) {
                float[] point=map.point(World.LON[port],World.LAT[port]);
                check(point[0]>=16 && point[0]<=344,"projected harbor horizontal bounds");
                check(point[1]>=24 && point[1]<=260,"projected harbor vertical bounds");
            }
        }
        check(World.PORTS[0].equals("리스본") && World.find("나가사키")>=40,"real port names");
        check(World.find("not-a-port")==-1,"unknown port lookup");
        for(int from=0;from<World.COUNT;from++) checkRoutes(from);
    }
    private static void checkLabels(MapProjection map) {
        MapLabels labels=new MapLabels(map);
        int[] ports=World.portsIn(map.region);
        for(int i=0;i<ports.length;i++) {
            float[] box=labels.boxes[i];
            check(box[0]>=16 && box[2]<=344 && box[1]>=24 && box[3]<=260,"map label bounds");
            check(labels.hit((box[0]+box[2])/2,(box[1]+box[3])/2)==ports[i],"label center selects its own port");
            for(int j=0;j<i;j++) check(!MapLabels.overlap(box,labels.boxes[j]),"map labels never overlap");
            float[] point=map.point(World.LON[ports[i]],World.LAT[ports[i]]);
            check(World.validPort(map.hit(point[0],point[1])),"touch can choose a regional port");
        }
    }
    private static void checkRoutes(int from) {
        for(int to=0;to<World.COUNT;to++) {
            Route route=WorldRoutes.between(from,to);
            check(route.ports[0]==from && route.ports[route.ports.length-1]==to,"route endpoints");
            check(from==to || route.miles>0,"all ports connected by sea graph");
            check(Math.abs(route.miles-WorldRoutes.between(to,from).miles)<.01,"routes symmetric");
        }
    }
    private static void coastRouteAndHighBits() {
        Route east=WorldRoutes.between(World.find("리스본"),World.find("캘리컷"));
        check(east.contains(World.find("케이프타운")),"Indian Ocean route goes around Cape, not through Egypt");
        Engine e=new Engine();
        for(int i=0;i<100;i++) e.work();
        int destination=World.find("부에노스아이레스");
        check(e.sail(destination,1),"can sail to far southern Americas");
        check(e.hasVisited(destination),"visit bit above 32 tracked");
        check(!e.hasVisited(World.find("고아")),"high visit bit does not alias low bit");
        check(e.invest(),"invest beyond legacy five ports");
        Engine restored=Engine.decode(e.encode());
        check(restored!=null && restored.hasVisited(destination),"high visit bit saved and restored");
        check(restored.influence[destination]==1,"global port influence saved");
        check(restored.encode().equals(e.encode()),"world save exact roundtrip");
    }
    private static void legacyMigration() throws Exception {
        String fixture=new String(Files.readAllBytes(Paths.get("tests/fixtures/v1-save.txt")),StandardCharsets.US_ASCII).trim();
        Engine e=Engine.decode(fixture);
        check(e!=null,"old installed prototype save loads");
        check(e.gold==1584 && e.day==5 && e.inventory[0]==5,"legacy possessions and day retained");
        check(e.port==World.find("세비야") && e.influence[e.port]==2,"legacy harbor translated");
        check(e.chapter==0 && e.message.contains("확장"),"new world story restart explained");
        check(Engine.decode(e.encode())!=null && e.encode().startsWith(SaveCodec.PREFIX),"migration upgrades to current save version");
    }
    private static void globalRandomActions() {
        Engine e=new Engine(); java.util.Random rng=new java.util.Random(1742);
        for(int i=0;i<2500;i++) {
            int action=rng.nextInt(7),good=rng.nextInt(6),amount=rng.nextInt(10)+1;
            switch(action) {
                case 0: e.buy(good,amount);break;
                case 1: e.sell(good,amount);break;
                case 2: e.sail(rng.nextInt(World.COUNT),rng.nextInt(2));break;
                case 3: e.repair();break;
                case 4: e.upgrade();break;
                case 5: e.invest();break;
                default: e.work();
            }
            check(Engine.decode(e.encode())!=null,"legal world actions produce valid saves");
            check(e.gold>=0 && e.cargo()<=e.capacity(),"funds/cargo invariants");
        }
    }
    public static void main(String[] args) throws Exception {
        geography();coastRouteAndHighBits();legacyMigration();globalRandomActions();
        System.out.println("PASS: "+checks+" world checks");
    }
}
