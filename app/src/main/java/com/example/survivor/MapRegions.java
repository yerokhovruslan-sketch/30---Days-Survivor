package com.example.survivor;

/** Hand-drawn, normalised boundaries; independent of screen size and zoom. */
final class MapRegions {
 static final String[] NAMES={"Северный лес и озёра","Карьер и горы","Городские кварталы","Военная территория","Промышленная зона","Южные кварталы","Южные дороги и озёра"};
 private static final float[][] BASE={
  {0,0, .56f,0, .56f,.22f, .50f,.30f, .38f,.36f, .27f,.34f, 0,.31f},
  {.56f,0, 1,0, 1,.26f, .76f,.27f, .56f,.30f, .50f,.30f, .56f,.22f},
  {0,.31f, .27f,.34f, .38f,.36f, .46f,.43f, .43f,.54f, .30f,.57f, .12f,.56f, 0,.52f},
  {.56f,.30f, .76f,.27f, 1,.26f, 1,.42f, .93f,.45f, .75f,.44f, .62f,.40f},
  {.38f,.36f, .50f,.30f, .56f,.30f, .62f,.40f, .75f,.44f, .93f,.45f, 1,.42f, 1,.68f, .82f,.69f, .72f,.64f, .60f,.64f, .55f,.58f, .43f,.54f, .46f,.43f},
  {0,.52f, .12f,.56f, .30f,.57f, .43f,.54f, .55f,.58f, .60f,.64f, .54f,.75f, .50f,.89f, .46f,1, 0,1},
  {.60f,.64f, .72f,.64f, .82f,.69f, 1,.68f, 1,1, .46f,1, .50f,.89f, .54f,.75f}
 };
 // Subdivide the same shared edges in a canonical direction, so neighbouring
 // regions retain exactly the same boundary and hit testing matches the artwork.
 static final float[][] SHAPES=buildShapes();
 private static float[][] buildShapes(){
  float[][] result=new float[BASE.length][];
  for(int z=0;z<BASE.length;z++){
   float[] p=BASE[z],out=new float[p.length*6];int k=0;
   for(int i=0;i<p.length;i+=2){int next=(i+2)%p.length;float ax=p[i],ay=p[i+1],bx=p[next],by=p[next+1];
    boolean reverse=ax>bx || ax==bx&&ay>by;float sx=reverse?bx:ax,sy=reverse?by:ay,ex=reverse?ax:bx,ey=reverse?ay:by;
    float dx=ex-sx,dy=ey-sy,len=(float)Math.hypot(dx,dy);boolean edge=(sx==0&&ex==0)||(sx==1&&ex==1)||(sy==0&&ey==0)||(sy==1&&ey==1);
    for(int j=0;j<6;j++){float t=(reverse?6-j:j)/6f;float wave=edge||len==0?0:(float)(Math.sin(Math.PI*t)*Math.sin(3*Math.PI*t)*Math.min(.0025,len*.035));
     out[k++]=sx+dx*t+(len==0?0:-dy/len*wave);out[k++]=sy+dy*t+(len==0?0:dx/len*wave);
    }
   }result[z]=out;
  }return result;
 }
 static boolean contains(int zone,float x,float y){
  float[] v=SHAPES[zone];boolean inside=false;
  for(int i=0,j=v.length-2;i<v.length;j=i,i+=2){float xi=v[i],yi=v[i+1],xj=v[j],yj=v[j+1];
   if((yi>y)!=(yj>y)&&x<(xj-xi)*(y-yi)/(yj-yi)+xi)inside=!inside;
  }return inside;
 }
 static int at(float x,float y){if(x<0||x>1||y<0||y>1)return -1;for(int i=0;i<SHAPES.length;i++)if(contains(i,x,y))return i;return -1;}
 static float center(int zone,boolean x){float[] v=SHAPES[zone];float min=1,max=0;for(int i=x?0:1;i<v.length;i+=2){min=Math.min(min,v[i]);max=Math.max(max,v[i]);}return (min+max)/2;}
 private MapRegions(){}
}
