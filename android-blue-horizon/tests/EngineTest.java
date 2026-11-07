package com.bluehorizon;

public final class EngineTest {
    private static int checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    private static void initialAndTrade() {
        Engine e = new Engine();
        check(e.gold == 1800 && e.port == 0, "starting harbor and gold");
        int price = e.buyPrice(0);
        check(e.buy(0, 5), "buy goods");
        check(e.gold == 1800 - price * 5 && e.cargo() == 5, "buy accounting");
        check(!e.buy(-1, 1) && !e.buy(0, -2), "reject invalid purchase");
        check(!e.buy(0, 9999), "reject over capacity");
        check(!e.sell(1, 1) && !e.sell(0, 6), "reject absent cargo");
        check(e.sell(0, 5) && e.cargo() == 0, "sell cargo");
        check(e.gold < 1800, "same port resale cannot generate profit");
    }
    private static void voyages() {
        Engine e = new Engine();
        check(!e.sail(0, 0) && !e.sail(World.COUNT, 0), "reject invalid destination");
        int cost = e.travelCost(1, 0);
        check(e.sail(1, 0), "sail to second port");
        check(e.gold == 1800 - cost && e.day > 1, "voyage consumes time and gold");
        check((e.visited & 2) != 0, "remember visited harbor");
        e.gold = 0;
        check(!e.sail(2, 0) && e.port == 1, "cannot travel without supplies");
        e.gold = 1000; e.hull = 1;
        check(!e.sail(2, 0), "damaged vessel cannot depart");
        check(e.repair() && e.hull == e.maxHull(), "repair ship");
    }
    private static void upgradesAndStory() {
        Engine e = new Engine();
        check(!e.upgrade(), "upgrade requires funds");
        e.gold = 10000;
        check(e.upgrade() && e.level == 2 && e.capacity() == 45, "upgrade increases hold");
        check(e.upgrade() && e.level == 3, "third ship");
        check(!e.upgrade(), "max ship level");
        check(e.invest() && e.influence[0] == 1, "invest in port");
        check(e.buy(0, 5) && e.sail(1, 0) && e.sell(0, 5), "first delivery");
        check(e.chapter == 1, "first story chapter completed");
        check(e.sellPrice(0) > e.buyPriceAt(0, 0), "wood route profitable");
        check(e.sail(2, 0) && e.sail(3, 0), "visit lighthouse ports");
        e.fame = 10; e.advanceStory();
        check(e.chapter == 2, "second story chapter");
        check(e.chapter < Story.REVEALS.length, "northern visit is not world ending");
    }
    private static void persistence() {
        Engine e = new Engine(); e.buy(0, 5); e.sail(1, 1);
        String saved = e.encode();
        Engine restored = Engine.decode(saved);
        check(restored != null && restored.encode().equals(saved), "save exact round trip");
        check(Engine.decode("broken") == null, "reject corrupt save");
        check(Engine.decode(saved.substring(0, saved.length() - 4)) == null, "reject truncated save");
        check(Engine.decode(null) == null, "reject missing save");
        e.gold = -1;
        check(Engine.decode(e.encode()) == null, "reject invalid saved state");
    }
    private static void safeguards() {
        Engine e = new Engine();
        check(!e.sail(1, 4), "reject unknown route mode");
        check(!e.repair(), "no charge for intact ship");
        e.gold = 0;
        check(e.work() && e.gold == 120 && e.day == 2, "recover from bankruptcy");
        check(e.work(), "port work repeatable");
        for (int i = 0; i < 4; i++) { e.gold = 10000; e.invest(); }
        check(e.influence[0] == 3 && !e.invest(), "investment capped");
        check(Engine.decode(e.encode()) != null, "max influence save valid");
    }
    private static void malformedStates() {
        Engine e=new Engine(); e.port=World.COUNT;
        check(Engine.decode(e.encode())==null,"invalid harbor rejected");
        e=new Engine(); e.level=0;
        check(Engine.decode(e.encode())==null,"invalid level rejected before capacity lookup");
        e=new Engine(); e.inventory[0]=26;
        check(Engine.decode(e.encode())==null,"overfull save rejected");
        e=new Engine(); e.inventory[1]=-1;
        check(Engine.decode(e.encode())==null,"negative inventory rejected");
        e=new Engine(); e.influence[0]=4;
        check(Engine.decode(e.encode())==null,"invalid influence rejected");
        e=new Engine(); e.port=2;
        check(Engine.decode(e.encode())==null,"unvisited current port rejected");
        e=new Engine(); e.chapter=Story.REVEALS.length+1;
        check(Engine.decode(e.encode())==null,"invalid chapter rejected");
        e=new Engine(); e.hull=101;
        check(Engine.decode(e.encode())==null,"invalid hull rejected");
        String good=new Engine().encode();
        check(Engine.decode(good.replace(SaveCodec.PREFIX, "BLUE99:"))==null,"unknown save version rejected");
        String[] parts=good.split(":",3);String payload=parts[2];
        parts[2]=(payload.charAt(0)=='A'?"B":"A")+payload.substring(1);
        check(Engine.decode(String.join(":",parts))==null,"checksum corruption rejected");
    }
    private static void tradingCircuit(Engine e) {
        if(e.port!=0) check(e.sail(0,1),"return to wood supplier");
        check(e.buy(0,20),"load profitable wood cargo");
        check(e.sail(1,1),"ship full cargo");
        check(e.sell(0,20),"sell profitable cargo");
        check(e.sail(0,1),"return for another load");
        check(e.repair(),"maintain ship after circuit");
    }
    private static void playableCampaign() {
        Engine e=new Engine();
        for(int i=0;i<9;i++) tradingCircuit(e);
        check(e.chapter==1 && e.fame>=10,"earn story reputation through play");
        check(e.upgrade(),"fund second ship through trading");
        sailFunded(e,2);sailFunded(e,3);
        check(e.chapter==2,"northern archives complete");
        sailFunded(e,13);check(e.invest(),"support Alexandrian archive");
        check(e.chapter==3,"Mediterranean chapter complete");
        sailFunded(e,21);check(e.chapter==4,"Cape chapter complete");
        sailFunded(e,28);check(e.buy(3,5),"buy Indian spices");
        sailFunded(e,32);check(e.sell(3,5) && e.chapter==5,"monsoon trade chapter");
        sailFunded(e,42);check(e.buy(4,5),"buy East Asian tea");
        sailFunded(e,44);check(e.sell(4,5) && e.chapter==6,"East Asian chapter");
        sailFunded(e,48);sailFunded(e,53);
        check(e.chapter==7,"Atlantic archives complete");
        while(e.gold<e.travelCost(0,1)+12000) e.work();
        sailFunded(e,0);
        check(e.chapter==Story.REVEALS.length,"global campaign finishes without state edits");
        check(Engine.decode(e.encode())!=null,"finished campaign save reloads");
    }
    private static void sailFunded(Engine e,int port) {
        while(e.gold<e.travelCost(port,1)+e.repairCost()+1500) e.work();
        if(e.hull<e.maxHull()) check(e.repair(),"repair before ocean crossing");
        check(e.sail(port,1),"fund and sail through normal port actions");
    }
    private static void randomizedActions() {
        Engine e=new Engine(); java.util.Random random=new java.util.Random(1741);
        for(int i=0;i<3000;i++) {
            int action=random.nextInt(7), good=random.nextInt(6), amount=random.nextInt(10)+1;
            switch(action) {
                case 0: e.buy(good,amount);break;
                case 1: e.sell(good,amount);break;
                case 2: e.sail(random.nextInt(5),random.nextInt(2));break;
                case 3: e.repair();break;
                case 4: e.upgrade();break;
                case 5: e.invest();break;
                default: e.work();
            }
            Engine restored=Engine.decode(e.encode());
            check(restored!=null,"legal actions always produce restorable state at step "+i);
            check(e.gold>=0 && e.cargo()<=e.capacity(),"funds and cargo invariant at step "+i);
        }
    }
    public static void main(String[] args) {
        initialAndTrade(); voyages(); upgradesAndStory(); persistence(); safeguards();
        malformedStates(); playableCampaign(); randomizedActions();
        System.out.println("PASS: " + checks + " game checks");
    }
}
