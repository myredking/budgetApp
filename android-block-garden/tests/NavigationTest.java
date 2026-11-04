package com.blockgarden;
public class NavigationTest {
 public static void main(String[] args){
  Navigation n=new Navigation();check(n.isMenu(),"launch shows menu");
  n.choose(false);check(!n.isMenu()&&!n.isMission(),"choose free");
  n.home();check(n.isMenu(),"return home");
  n.choose(true);check(!n.isMenu()&&n.isMission(),"choose mission");
  n.home();check(n.isMenu(),"mission return");
  check(new Navigation().isMenu(),"new launch ignores last mode");
 }
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
