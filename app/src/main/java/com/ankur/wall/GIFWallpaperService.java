package com.ankur.wall;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Movie;
import android.graphics.drawable.AnimatedImageDrawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.core.content.ContextCompat;


import com.ankur.wall.R;

import java.io.IOException;

/**
 * GIF WALLPAPER SERVICE EXTENDING WALLPAPER SERVICE
 */
public class GIFWallpaperService extends WallpaperService {
    /**
     * Returns a custom made engine for wallpaper service
     *
     * @return GIFWALLPAPER Custom Engine
     */
    @Override
    public WallpaperService.Engine onCreateEngine() {
        try {
            //Loading Gif
            ImageDecoder.Source src = ImageDecoder.createSource
                    (getAssets(), "milk_mocha_dance.gif");
            AnimatedImageDrawable movie = (AnimatedImageDrawable) ImageDecoder.decodeDrawable(src);

            //Passing the GIFs as parameters of the Engine and returning the Engine
            return new GIFWallpaperEngine(movie);

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("GIF : ", "Could not load GIF");
            return null;
        }
    }

    /**
     * Wallpaper Engine
     */
    private class GIFWallpaperEngine extends WallpaperService.Engine {

        //Holder to hold a display view
        private SurfaceHolder holder;
        //Image Drawable to load GIF
        private AnimatedImageDrawable movie;
        //A Handler allows you to send and process Message and
        // Runnable objects associated with a thread's MessageQueue.
        private android.os.Handler handler;
        //Interface to run canvas created
        private Runnable drawGIF;

        private AudioManager manager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);


        /**
         * Constructor Function of the Engine Class
         *
         * @param movie Gif to Set as Wallpaper
         */
        public GIFWallpaperEngine(AnimatedImageDrawable movie) {
            //Initialising Drawable variables with passed parameters
            this.movie = movie;
            //Initialisation of the handler
            handler = new Handler(Looper.getMainLooper());
        }

        /**
         * OnCreate method
         *
         * @param surfaceHolder default Parameter
         */
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            //Initialising the holder variable
            this.holder = surfaceHolder;

            //Initialising the Runnable
            drawGIF = new Runnable() {
                //Must method to be defined for a runnable
                @Override
                public void run() {
                    //Passing the method to execute while running
                    draw();

                }
            };

            //Causes the runnable to be added to the message queue.
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    //movie2.start() not included to have a static frame when music stops
                    movie.start();
                }
            });
        }

        /**
         * Called to inform you of the wallpaper becoming visible or hidden.
         * It is very important that a wallpaper only use CPU while it is visible
         *
         * @param visible contains True or False value if wallpaper is visible or not
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            //If wallpaper Visible then
            if (visible) {
                //Add runnable drawGIF to message Queue
                handler.post(drawGIF);
            } else {
                //Else Remove pending posts of runnable
                handler.removeCallbacks(drawGIF);
            }
        }

        /**
         * Called right before Engine is going away
         */
        @Override
        public void onDestroy() {
            super.onDestroy();
            //Remove pending posts of runnable
            handler.removeCallbacks(drawGIF);
        }

        /**
         * Called immediately after any structural changes have been made to the surface.
         * You should at this point update the imagery in the surface.
         *
         * @param holder The SurfaceHolder whose surface has changed.
         * @param format The new PixelFormat of the surface.
         * @param width  The new width of the surface. Value is 0 or greater
         * @param height The new height of the surface. Value is 0 or greater
         */
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            //Method to update according to the new width and height of the Surface
            updateScaleAndPadding2(width, height);
            //Add runnable drawGIF to message Queue of the new Surface.
            handler.post(drawGIF);
        }

        /**
         * Called to inform you of the wallpaper's offsets changing within its contain
         */
        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
            handler.post(drawGIF);
        }

        //Variable to Scale the size of GIF according to the Screen
        float scale = -1;

        /**
         * Method to draw the GIF on Canvas
         */
        public void draw() {
            // since get gif with AnimatedImageDrawable must be in handler.post, so gif maybe null
            if (movie != null) {
                //Creating a null valued canvas to draw upon
                Canvas canvas = null;

                try {
                    //Updating the Scale initial check while creating the first frame
                    if (scale == -1) {
                        updateScaleAndPadding();
                    }
                    //Surface Holder method to lock the canvas and start editing the pixels
                    // to draw into the surface
                    canvas = holder.lockCanvas();

                    //If there's already a frame on the Canvas
                    if (canvas != null) {
                        //Moves the print head along the X and Y axes the no. of pixels
                        canvas.translate(horiPadding, vertiPadding);
                        //Scale the Canvas according to the Updated Scale Value
                        canvas.scale(scale, scale);
                        //Canvas Background color
                        canvas.drawColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
                        //Draw the Frame on the Canvas
                        if (manager.isMusicActive()) {
                            //if music is playing then gif animating
                            movie.draw(canvas);
                            movie.start();
                        } else {
                            //if music is not playing then a static frame
                            movie.stop();
                            movie.draw(canvas);
                        }
                    }
                } finally {
                    if (canvas != null) {
                        //After this call, the surface's current pixels will be shown on the screen
                        holder.unlockCanvasAndPost(canvas);
                    }
                }
            }

            //Remove pending posts of runnable
            handler.removeCallbacks(drawGIF);
            // To determine the frames per second of the animation
            float fps = 60;
            //Causes the Runnable to be run after the specified amount of time elapses.
            handler.postDelayed(drawGIF, (long) (1000L / fps));
        }

        //Variables to get X and Y values from where the drawing should start on the canvas
        int horiPadding;
        int vertiPadding;

        /**
         * Method to Update Scale and Padding of the Canvas
         */
        private void updateScaleAndPadding() {
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                //Getting Height and Width of the Canvas
                int cw = canvas.getWidth();
                int ch = canvas.getHeight();

                //Updating According to the Height and Width of canvas
                updateScaleAndPadding2(cw, ch);
            } finally {
                if (canvas != null) {
                    //If not null then show the current pixels on screen
                    holder.unlockCanvasAndPost(canvas);
                }
            }
        }

        /**
         * Method to Update Scale. Used as a Sub Method
         *
         * @param cw Canvas Width
         * @param ch Canvas Height
         */
        public void updateScaleAndPadding2(int cw, int ch) {
            // since get gif with AnimatedImageDrawable must be in handler.post, so gif maybe null
            if (movie != null) {
                //Variables to get Height and Width of the GIF
                int gifW;
                int gifH;
                gifW = movie.getIntrinsicWidth();
                gifH = movie.getIntrinsicHeight();

                //Scaling To fit GIF on the Canvas
                if (gifW * 1f / gifH > cw * 1f / ch) {
                    scale = cw * 1f / gifW;
                } else {
                    scale = ch * 1f / gifH;
                }

                //Getting Initial Values to start drawing
                horiPadding = (int) ((cw - gifW * scale) / 2);
                vertiPadding = (int) ((ch - gifH * scale) / 2);
            }
        }
    }
}
