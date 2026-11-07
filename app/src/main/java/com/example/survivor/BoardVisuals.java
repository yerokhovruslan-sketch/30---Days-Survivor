package com.example.survivor;
import com.example.survivor.game.BattleBoard;
import java.util.*;
/** Orthogonal paths only. Preview and interpolation never cross solid cells. */
public final class BoardVisuals {
 public static List<Integer> path(BattleBoard board,int from,int to){
  int[] prev=new int[64];Arrays.fill(prev,-1);prev[from]=from;ArrayDeque<Integer> q=new ArrayDeque<>();q.add(from);
  while(!q.isEmpty()){
   int at=q.remove();if(at==to)break;
   for(int d:new int[]{-8,8,-1,1}){int n=at+d;if(n<0 || n>=64 || Math.abs(n/8-at/8)+Math.abs(n%8-at%8)!=1 || prev[n]>=0 || board.cells.get(n).obstacle>=0)continue;prev[n]=at;q.add(n);}
  }
  List<Integer> result=new ArrayList<>();if(prev[to]<0)return result;
  for(int n=to;;n=prev[n]){result.add(n);if(n==from)break;}Collections.reverse(result);return result;
 }
}
