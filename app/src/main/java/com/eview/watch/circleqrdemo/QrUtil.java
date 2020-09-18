package com.eview.watch.circleqrdemo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;

public class QrUtil {
    /**
     * 绘制一个模拟的二维码，二维码的图案是随机填充的
     * @param canvas
     * @param shortest 模拟二维码小格子的宽度
     * @param width 模拟二维码的宽度
     */
    private  static void drawDefQr(Canvas canvas, int shortest, int width){
        Paint paint = new Paint();
        for(int y = 0;y<width;){
            for(int x= 0;x<width;){
                int select = ((int) (Math.random()*10))&0x01;
                paint.setColor(select==1? Color.BLACK: Color.WHITE);
                canvas.drawRect(x,y,x+shortest,y+shortest,paint);
                x = x+shortest;
            }
            y = y+shortest;
        }
    }

    /**
     * 把left/top margin微调到格子的整数倍，以便模拟二维码和实际二维码之间没有缝隙
     * @param sourceMargin
     * @param cubSize
     * @return
     */
    private  static int wrapDrawMargin(int sourceMargin,int cubSize){
        int left = sourceMargin%cubSize;
        return sourceMargin-left;
    }
    public static Bitmap createBindQr(String url, Bitmap icBitmap, float imageScale, int width){
        //算出来以设定宽度的圆形的内接正方形的宽度，就是二维码的宽度，二维码会被放在圆形背景上面
        //得到方形二维码的位图，并且计算了方形二位码的小格子的宽度和默认的margin以后，再绘制一个模拟的二维码，按照计算得到的方形二维码小格子的尺寸填充整个背景区域
        //然后把方形二维码贴在模拟二维码上
        //接着把logo贴上去
        //接着把图片切成圆形，然后绘制上圆形的边框

        //算出来以设定宽度的圆形的内接正方形的宽度，就是二维码的宽度，二维码会被放在圆形背景上面

        int targetWidth  = (int) (width*1.4142f/2);
        int[] params = new int[2];//这里0 保存的是获取的二维码小格子的宽度，1保存的是方形二维码的边框的宽度
        //生成二维码原图（步骤1）
        Bitmap qrBitmap = createQrBitmap(url,targetWidth,params);

        if(qrBitmap!=null) {
            //生成一个最终要返回的位图
            Bitmap bitmap = Bitmap.createBitmap(width,width, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            //根据步骤1返回的参数绘制有一个模拟的方形二维码
            drawDefQr(canvas,params[0],width);
            //生成的二维码的边框宽度
            int qrMargin = params[1];
            //要绘制的实际的二维码的宽度，去掉边框
            targetWidth = targetWidth-params[1]*2;

            //待绘制二维码在背景中实际的左边距和上边距
            int drawMargin = (width-targetWidth)/2;
            //为了待位置二维码和作为背景的模拟二维码格子能无缝拼接，需要微调一下边距，确保边距是格子的整数倍
            drawMargin =  wrapDrawMargin(drawMargin,params[0]);
            //计算原二维码位图要绘制的区域，这个区域要排除边框的宽度
            Rect source = new Rect(qrMargin,qrMargin,qrMargin+targetWidth,qrMargin+targetWidth);
            //计算待绘制的二维码位图在画布上的位置
            Rect target = new Rect(drawMargin,drawMargin,drawMargin+targetWidth,drawMargin+targetWidth);
            //把实际的二维码绘制到画布上
            canvas.drawBitmap(qrBitmap,source,target,paint);

            //绘制图标
            int icWidth = (int)(width*imageScale);
            int margin = (int) ((1-imageScale)/2f*width);
            Rect rect = new Rect(margin,margin,margin+icWidth,margin+icWidth);
            canvas.drawBitmap(icBitmap,null,rect,paint);

            //绘制圆形边框
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            float radius = width/2f-2.5f;
            paint.setColor(Color.WHITE);
            canvas.drawCircle(width/2f,width/2f,radius,paint);
            //把画布切成圆形
            Bitmap circleBitmap = CropCircleHelper.cropCircle(bitmap,width);
            return circleBitmap;
        }
        return null;

    }

    /**
     *
      * @param url 二维码的内容
     * @param width 二维码绘制的宽度
     * @param shortest 用来乘放二维码位图中相关的尺寸参数 0 保存的是获取的二维码小格子的宽度，1保存的是方形二维码的边框的宽度
     * @return 二维码位图，包含边框
     */
    public static Bitmap createQrBitmap(String url, int width, int[] shortest){
        try {
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET,"utf-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN,0);
            BitMatrix byteMatrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE,width,width,hints);
            int[] pixels = new int[width*width];
            int shortestPix = 0;
            boolean lastSelected = false;
            int currentShortest = width;
            int consequenceSelected = 0;
            int tempForMargin = 0;
            int temptConsequenceSelected = 0;
            boolean allUnSelected = true;
            for(int y = 0;y<width;y++){
                allUnSelected = true;
                for(int x = 0;x<width;x++){
                    boolean selected = byteMatrix.get(x,y);
                    //把选中状态记录到bitmap中，用颜色来表示
                    pixels[y*width+x]=byteMatrix.get(x,y)? Color.WHITE: Color.BLACK;
                    //记录这一行是不是全部未选中，如果全部未选中，说明这一行是上边框或下边框，连续未选中行数可以确定上边框或下边框的宽度
                    if(selected){
                        allUnSelected = false;
                    }
                    //如果margin找到了，说明接下来的才是真正二维码的内容
                    if(shortest[1]>0) {
                        //***********这些逻辑是为了找到单个二维码格子的宽度,最小的连续格子数就是二维码最小格子的宽度
                        //如果是第一列，记录初始的选中状态
                        if(x<shortest[1]||x>=width-shortest[1]) {
                            //排除掉横向在绘制margin的情况
                            continue;
                        }
                        if (x == shortest[1]) {//在每行的第一个像素开始记录最初的宽度
                            lastSelected = selected;
                            shortestPix = 1;
                        } else if (selected == lastSelected) {
                            shortestPix++;//如果选中状态不变，说明是同一个格子
                        } else {
                            if (shortestPix > 1) {
                                if (shortestPix < currentShortest) {
                                    //如果有比上一次计算得到的更小的格子，就记录当前值
                                    currentShortest = shortestPix;
                                }
                            }
                            lastSelected = selected;
                            shortestPix = 1;
                        }
                    }


                }
                if(consequenceSelected==0&&allUnSelected){
                    tempForMargin++;
                }else  if(tempForMargin>0&&consequenceSelected==0){
                    consequenceSelected = tempForMargin;
                    shortest[1] = consequenceSelected;
                }

            }

            Log.d("createQrBitmap","currentShortest "+currentShortest+" margin "+shortest[1]);
            shortest[0] = currentShortest;
            Bitmap bitmap = Bitmap.createBitmap(width,width, Bitmap.Config.ARGB_8888,true);
            bitmap.setPixels(pixels,0,width,0,0,width,width);
            return bitmap;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
