package com.bluehorizon;

/** Optional, finite local stories and regional companion moments. */
public final class PortLife {
    private static final String[] TITLES={"물에 젖은 장부","바람에 꺾인 표지판","출발을 기다리는 편지","잃어버린 저울추","작은 부두의 약속","밤을 밝히는 등불","오래된 항구 그림","돌아온 견습 선원"};
    private static final String[] SCENES={
        "부두 서기가 젖은 거래 장부를 말리고 있다. 이름이 지워지면 작은 상인들의 품삯도 사라진다고 한다.",
        "길 안내판이 강풍에 부러져 처음 온 선원들이 길을 잃었다. 목공은 손을 보탤 사람을 찾는다.",
        "우편 담당자가 떠날 배를 놓쳤다. 내일까지 편지를 분류하면 다음 배에 실을 수 있다고 한다.",
        "시장의 저울추가 뒤섞였다. 상인들은 서로를 의심하기 전에 모두 함께 무게를 확인해 달라고 부탁한다.",
        "짐꾼들이 다친 동료 몫까지 나르느라 지쳐 있다. 선장은 선원들과 함께 도울지 잠시 고민한다.",
        "등불 관리인이 기름통을 옮기다 발을 다쳤다. 오늘 밤 작은 배들이 돌아오기 전에 준비해야 한다.",
        "그림을 그리는 아이가 사라진 부두의 옛 모습을 찾는다. 창고 기록을 정리하면 단서를 찾을 수 있다.",
        "긴 항해에서 돌아온 견습 선원이 첫 짐 정리를 맡았다. 서툴지만 끝까지 책임지고 싶다고 한다."
    };
    private static final String[] LOCAL={
        "차가운 바람에 창고 문이 덜컹이고, 모직 외투를 입은 사람들이 모여든다.",
        "돌바닥 광장에 종소리가 퍼지고, 올리브 향이 나는 가게들이 문을 연다.",
        "그늘막 아래 사람들이 물을 나누고, 해풍에 묶인 천이 펄럭인다.",
        "계절풍을 기다리는 선원들 곁으로 향신료 자루가 지나간다.",
        "소나기가 갠 수로에 작은 운반선들이 다시 모여든다.",
        "찻집의 김이 부두로 흘러나오고, 도자기 상자를 나르는 손길이 조심스럽다.",
        "색색의 벽 사이로 음악이 흐르고, 먼 항구에서 온 선원들이 안부를 나눈다."
    };
    private static final String[][] TOPICS={
        {"이안은 젖은 장부를 보며 작은 상인의 이름도 해도에 남기자고 한다.","이안은 광장에서 들은 서로 다른 말을 장부 여백에 적는다.","이안은 물 한 잔을 나눈 사람의 이름을 오래 기억하고 싶어 한다.","이안은 계절풍을 기다리는 동안 거래 장부 대신 여행기를 쓴다.","이안은 작은 배의 선장에게도 손님이 아닌 동료로 인사한다.","이안은 찻집의 느린 대화가 가격표보다 많은 것을 알려 준다고 한다.","이안은 노랫가락 속 항구 이름들을 새 장부의 첫 장에 적는다."},
        {"미라는 흐린 밤에도 별을 찾는 어부의 방법을 배우고 싶어 한다.","미라는 종탑 그림자로 항구의 시간을 재어 본다.","미라는 해안의 바람과 구름을 함께 기록한다.","미라는 계절풍의 방향이 바뀌는 날을 현지 선원과 비교한다.","미라는 섬마다 다른 조류 표식을 하나씩 그린다.","미라는 오래된 별자리 기록과 자신의 관측을 나란히 펼친다.","미라는 처음 보는 남쪽 별을 가리키며 잠시 말을 잊는다."},
        {"나디아는 항구 도서관의 책에 남은 수많은 손자국을 살핀다.","나디아는 같은 지명을 다르게 적은 옛 지도 두 장을 비교한다.","나디아는 말로 전해 온 항로도 기록만큼 소중하다고 한다.","나디아는 향신료 이름의 어원을 알려 준 상인에게 감사를 전한다.","나디아는 여러 언어로 적힌 약속을 한 장씩 베껴 쓴다.","나디아는 인쇄된 지도와 손으로 고친 메모 중 어느 쪽도 버리지 않는다.","나디아는 노래로 남은 항해 이야기를 글로 옮겨도 되는지 먼저 묻는다."},
        {"타보는 추위에 굳은 선원의 손을 보고 장갑부터 챙긴다.","타보는 바쁜 광장에서 잠시 조용히 쉴 자리를 찾는다.","타보는 물통을 나르는 일을 가장 힘든 동료와 나누겠다고 한다.","타보는 긴 항해에 앞서 선원들의 잠자리부터 점검한다.","타보는 갑작스러운 비를 피해 모두 함께 웃을 수 있어 다행이라고 한다.","타보는 따뜻한 차 한 잔이 먼 길의 피로를 덜어 준다고 한다.","타보는 돌아갈 고향이 다른 동료들도 같은 배의 식구라고 한다."},
        {"아유는 거친 바람에도 약속한 날에 문을 연 상인을 기억한다.","아유는 흥정이 끝난 뒤 나눈 인사가 더 오래 남는다고 한다.","아유는 물을 나누는 규칙을 먼저 배우고 거래를 시작한다.","아유는 바람을 탓하지 않고 다음 약속을 정하는 선장을 존중한다.","아유는 수로의 작은 가게에서 자기 고향과 닮은 향을 찾는다.","아유는 차를 권하는 순서에도 상대를 생각하는 마음이 담겼다고 한다.","아유는 발견한 항로의 이익을 현지 사람과 나눌 방법을 고민한다."}
    };
    public static boolean done(Engine e) { return (e.life.events & (1L<<e.port))!=0; }
    public static String title(Engine e) { return World.PORTS[e.port]+" · "+TITLES[e.port%8]; }
    public static String story(Engine e) { return title(e)+"\n\n"+LOCAL[World.region(e.port)]+"\n\n"+SCENES[e.port%8]; }
    public static boolean choose(Engine e,int choice) {
        if(e.actionBlocked() || choice<0 || choice>1) return Adventure.fail(e,"항해와 사건을 마친 뒤 선택하세요.");
        if(done(e)) return Adventure.fail(e,"이 항구의 작은 이야기는 이미 마쳤습니다.");
        if(choice==0 && !Adventure.spend(e,40)) return false;
        e.life.events|=1L<<e.port;
        if(choice==0) { e.life.generous|=1L<<e.port;e.fame+=2;e.town.morale=Math.min(100,e.town.morale+8); }
        else { e.gold+=90;e.fame++;e.town.morale=Math.min(100,e.town.morale+2); }
        GameClock.advance(e,1);Rivals.advance(e);
        e.note(title(e)+(choice==0?" — 선원들과 무상으로 도왔다. 식사비 −40 G, 명성 +2, 사기 +8.":" — 약속한 일을 마쳤다. 품삯 +90 G, 명성 +1, 사기 +2.")+" 하루 경과.");
        e.advanceStory();return true;
    }
    private static long talkBit(Engine e,int person) { return 1L<<(World.region(e.port)*5+person-1); }
    public static boolean talked(Engine e,int person) { return person>=1 && person<=5 && (e.life.chats & talkBit(e,person))!=0; }
    public static String topic(Engine e,int person) { return TOPICS[person-1][World.region(e.port)]; }
    public static boolean recorded(Engine e,int person) { return (e.life.practical & talkBit(e,person))!=0; }
    public static String reply(int person,int choice) {
        String[] listening={"그럼 이 장부에는 우리가 만난 사람의 이름도 적을게. 함께 기억해 줘서 고마워.","당장 답을 찾지 못해도 괜찮겠어요. 선장님과 같은 하늘을 보고 있으니까요.","기록에 없는 목소리도 놓치지 않을게요. 오늘은 먼저 듣고 싶어요.","쉬어 갈 자리가 있다는 걸 모두 알았으면 해. 다음 항해도 함께 가자.","서로를 이해하는 시간이 좋은 거래의 시작이겠죠. 조금 더 머물러요."};
        String[] recording={"좋아. 다음에 돌아왔을 때 오늘의 약속을 다시 읽어 보자.","관측 날짜와 바람을 함께 남겨 둘게요. 다음 항해에 분명 도움이 되겠어요.","누구에게 들었는지도 함께 적을게요. 지식은 이름 없이 생기지 않으니까요.","선원들의 상태도 항해일지에 남기자. 다음 출항 준비에 꼭 쓰일 거야.","우리가 나눈 약속을 적어 둘게요. 먼 곳에서도 지킬 수 있도록요."};
        return (choice==0?listening:recording)[person-1];
    }
    public static boolean talk(Engine e,int person,int choice) {
        if(e.actionBlocked() || person<1 || person>5 || choice<0 || choice>1) return Adventure.fail(e,"지금은 대화할 수 없습니다.");
        if(!Crew.available(person,e.chapter) || talked(e,person)) return Adventure.fail(e,"이 해역의 대화를 이미 나눴거나 아직 합류하지 않았습니다.");
        long bit=talkBit(e,person);e.life.chats|=bit;
        if(choice==0) e.town.morale=Math.min(100,e.town.morale+5);else { e.life.practical|=bit;e.fame++; }
        e.note(World.PORTS[e.port]+"에서 "+Crew.NAMES[person]+"와 "+(choice==0?"이야기에 귀를 기울였다. 사기 +5.":"함께 기록을 남겼다. 명성 +1."));
        e.advanceStory();return true;
    }
}
