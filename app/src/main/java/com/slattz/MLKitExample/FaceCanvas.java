package com.slattz.MLKitExample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.util.List;
import java.util.Locale;

public class FaceCanvas extends View {
    Face face = null;
    Paint contourPaint;
    Paint outlinePaint;
    Paint textPaint;
    Paint textBGPaint;
    float scaleFactor = 1.0f;
    float postScaleWidthOffset;
    float postScaleHeightOffset;
    int imageHeight = 0;
    int imageWidth = 0;
    boolean isFlipped = true;

    final float TEXT_SIZE = 30.f;
    final float TEXT_BOX = 5.f;

    public FaceCanvas(Context context) {
        super(context);
        outlinePaint = new Paint();
        outlinePaint.setColor(Color.RED);
        outlinePaint.setStrokeWidth(5);
        outlinePaint.setStyle(Paint.Style.STROKE);

        contourPaint = new Paint();
        contourPaint.setColor(Color.WHITE);
        contourPaint.setStrokeWidth(5);
        contourPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(TEXT_SIZE);

        textBGPaint = new Paint();
        textBGPaint.setColor(Color.GRAY);
        textBGPaint.setStyle(Paint.Style.FILL);
    }

    private float scale(float imagePixel) {
        return imagePixel * scaleFactor;
    }

    private float translateX(float x) {
        if (isFlipped) {
            return getWidth() - (scale(x) - postScaleWidthOffset);
        } else {
            return scale(x) - postScaleWidthOffset;
        }    }

    private float translateY(float y) {
        return scale(y) - postScaleHeightOffset;
    }

    private void updateTransformation() {
        float viewAspectRatio = (float) getWidth() / getHeight();
        float imageAspectRatio = (float) this.imageWidth / this.imageHeight;
        postScaleWidthOffset = 0;
        postScaleHeightOffset = 0;
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = (float) getWidth() / this.imageWidth;
            postScaleHeightOffset = ((float) getWidth() / imageAspectRatio - getHeight()) / 2;
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = (float) getHeight() / this.imageHeight;
            postScaleWidthOffset = ((float) getHeight() * imageAspectRatio - getWidth()) / 2;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (this.face == null)
            return;

        updateTransformation();


        float x = translateX(face.getBoundingBox().centerX());
        float y = translateY(face.getBoundingBox().centerY());
        canvas.drawCircle(x, y, 8.0f, contourPaint);

        float left = x - scale(face.getBoundingBox().width() / 2.0f);
        float top = y - scale(face.getBoundingBox().height() / 2.0f);
        float right = x + scale(face.getBoundingBox().width() / 2.0f);
        float bottom = y + scale(face.getBoundingBox().height() / 2.0f);

        canvas.drawRect(left, top, right, bottom, outlinePaint);

        for (FaceContour contour : face.getAllContours()) {
            for (PointF point : contour.getPoints()) {
                canvas.drawCircle(translateX(point.x), translateY(point.y), 8.0f, contourPaint);
            }
        }

        String leftEyeProb = String.format(Locale.ENGLISH, "Left eye open probability: %.2f", face.getLeftEyeOpenProbability());
        String rightEyeProb = String.format(Locale.ENGLISH, "Right eye open probability: %.2f", face.getRightEyeOpenProbability());
        String smileProb = String.format(Locale.ENGLISH, "Smiling probability: %.2f", face.getSmilingProbability());
        String angleX = String.format(Locale.ENGLISH, "Head Angle X: %.2f", face.getHeadEulerAngleX());
        String angleY = String.format(Locale.ENGLISH, "Head Angle Y: %.2f", face.getHeadEulerAngleY());
        String angleZ = String.format(Locale.ENGLISH, "Head Angle Z: %.2f", face.getHeadEulerAngleZ());

        canvas.drawRect(left, top - (6*(TEXT_BOX+TEXT_SIZE)), right, top, textBGPaint);
        canvas.drawText(leftEyeProb, left, top - (5*(TEXT_BOX+TEXT_SIZE)), textPaint);
        canvas.drawText(rightEyeProb, left, top - (4*(TEXT_BOX+TEXT_SIZE)), textPaint);
        canvas.drawText(smileProb, left, top - (3*(TEXT_BOX+TEXT_SIZE)), textPaint);
        canvas.drawText(angleX, left, top - (2*(TEXT_BOX+TEXT_SIZE)), textPaint);
        canvas.drawText(angleY, left, top - (1*(TEXT_BOX+TEXT_SIZE)), textPaint);
        canvas.drawText(angleZ, left, top - (0*(TEXT_BOX+TEXT_SIZE)), textPaint);
    }

    public void setFace(Face face, int imageWidth, int imageHeight, boolean isFlipped) {
        this.face = face;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.isFlipped = isFlipped;
        invalidate();
    }

    public void resetFace() {
        face = null;
        invalidate();
    }
}
