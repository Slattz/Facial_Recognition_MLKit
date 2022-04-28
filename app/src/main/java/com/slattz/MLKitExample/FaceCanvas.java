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

public class FaceCanvas extends View {
    Face face = null;
    Paint paint;
    float scaleFactor = 1.0f;
    float postScaleWidthOffset;
    float postScaleHeightOffset;
    int imageHeight = 0;
    int imageWidth = 0;
    boolean isFlipped = true;


    public FaceCanvas(Context context) {
        super(context);
        paint = new Paint();
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
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);

        float x = translateX(face.getBoundingBox().centerX());
        float y = translateY(face.getBoundingBox().centerY());
        canvas.drawCircle(x, y, 8.0f, paint);

        for (FaceContour contour : face.getAllContours()) {
            for (PointF point : contour.getPoints()) {
                canvas.drawCircle(translateX(point.x), translateY(point.y), 8.0f, paint);
            }
        }

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
