package com.bluehorizon;

/** Approximate port coordinates and designed game economies; not a navigation chart. */
public final class World {
    public static final String[] REGIONS={"북해·서유럽","지중해","아프리카","인도양","동남아시아","동아시아","아메리카"};
    public static final String[] PORTS={
        "리스본","세비야","런던","암스테르담","함부르크","스톡홀름","오슬로","낭트",
        "마르세유","제노바","베네치아","아테네","이스탄불","알렉산드리아","튀니스","알제",
        "카사블랑카","아르긴","카보베르데","상투메","루안다","케이프타운","소팔라","모잠비크",
        "아덴","무스카트","호르무즈","고아","캘리컷","콜롬보","몸바사","말린디",
        "말라카","아체","팔렘방","바타비아","마카사르","마닐라","반텐","딜리",
        "마카오","취안저우","항저우","부산","나가사키","나하","호이안","단수이",
        "하바나","산토도밍고","산후안","베라크루스","카르타헤나","페르남부쿠","바이아","부에노스아이레스"
    };
    public static final int COUNT=PORTS.length;
    public static final float[] LON={
        -9.14f,-6.00f,-.13f,4.90f,9.99f,18.07f,10.75f,-1.55f,
        5.37f,8.93f,12.33f,23.73f,28.98f,29.92f,10.18f,3.06f,
        -7.59f,-16.45f,-23.51f,6.73f,13.23f,18.42f,34.72f,40.74f,
        45.03f,58.41f,56.46f,73.83f,75.78f,79.86f,39.67f,40.12f,
        102.25f,95.32f,104.76f,106.85f,119.41f,120.98f,106.15f,125.58f,
        113.54f,118.68f,120.16f,129.08f,129.87f,127.68f,108.33f,121.44f,
        -82.37f,-69.93f,-66.10f,-96.13f,-75.48f,-34.88f,-38.50f,-58.38f
    };
    public static final float[] LAT={
        38.72f,36.55f,51.51f,52.37f,53.55f,59.33f,59.91f,47.22f,
        43.30f,44.41f,45.44f,37.94f,41.01f,31.20f,36.81f,36.75f,
        33.59f,20.60f,14.92f,.34f,-8.84f,-33.92f,-20.18f,-15.04f,
        12.79f,23.59f,27.06f,15.49f,11.26f,6.93f,-4.04f,-3.22f,
        2.19f,5.55f,-2.99f,-6.21f,-5.15f,14.60f,-6.04f,-8.56f,
        22.20f,24.87f,30.25f,35.18f,32.75f,26.21f,15.88f,25.17f,
        23.11f,18.49f,18.47f,19.17f,10.39f,-8.05f,-12.97f,-34.60f
    };
    public static final int[] LEGACY_PORTS={0,1,13,28,42};
    public static boolean validPort(int port) { return port>=0 && port<COUNT; }
    public static int region(int port) { return port/8; }
    public static int find(String name) {
        for(int i=0;i<COUNT;i++) if(PORTS[i].equals(name)) return i;
        return -1;
    }
    public static int[] portsIn(int region) {
        if(region<0) { int[] all=new int[COUNT];for(int i=0;i<COUNT;i++) all[i]=i;return all; }
        int[] ports=new int[8];for(int i=0;i<8;i++) ports[i]=region*8+i;return ports;
    }
    public static int price(int port,int good) {
        if(port==0 && good==0) return 24;
        if(port==1 && good==0) return 65;
        int[] base={65,80,115,145,100,190};
        int[] regional={0,2,0,3,3,4,5};
        int specialty=regional[region(port)];
        if(good==specialty) return 28+(port%8)*2;
        if(good==(specialty+1)%6) return 42+(port%4)*4;
        return base[good]+((port*13+good*7)%35);
    }
    public static String marketTip(int port) {
        int good=0;
        for(int i=1;i<6;i++) if(price(port,i)<price(port,good)) good=i;
        return "현지 저가 상품: "+Engine.GOODS[good]+" ("+price(port,good)+" G)\n"
            +"가격은 항구별로 고정입니다. 먼 항해에는 보급비가 더 들어요. 출항 창에서 경유 항구와 비용을 확인하세요.\n\n"
            +"첫 무역 추천: 리스본 목재 → 세비야. 금화가 부족하면 조선소에서 항구 일거리를 이용하세요.";
    }
}
