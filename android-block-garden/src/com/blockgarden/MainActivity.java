package com.blockgarden;
import android.app.Activity;
import android.os.Bundle;
public class MainActivity extends Activity {
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(0xff101c30);getWindow().setNavigationBarColor(0xff101c30);setContentView(new GameView(this));}
}
