package com.upgenicsint.phonecheck.views

import android.util.Log

/**
 * This data model tracks the width and height of the playing field along
 * with the current position of a ball.
 */
class BouncingBallModel(val ballRadius: Int) {

    private var initialDrawOffset: Boolean = false
    // the ball speed is meters / second. When we draw to the screen,
    // 1 pixel represents 1 meter. That ends up too slow, so multiply
    // by this number. Bigger numbers speeds things up.
    private val pixelsPerMeter = 60f

    private var leftEdge: Boolean = false
    private var rightEdge: Boolean = false
    private var topEdge: Boolean = false
    private var bottomEdge: Boolean = false
    private var isTouchAllCorners: Boolean = false

    // these are public, so make sure you synchronize on LOCK
    // when reading these. I made them public since you need to
    // get both X and Y in pairs, and this is more efficient than
    // getter methods. With two getters, you'd still need to
    // synchronize.
    var ballPixelX: Float = 0.toFloat()
    var ballPixelY: Float = 0.toFloat()

    private var pixelWidth: Int = 0
    private var pixelHeight: Int = 0

    // values are in meters/second
    private var velocityX: Float = 0.toFloat()
    private var velocityY: Float = 0.toFloat()

    // typical values range from -10...10, but could be higher or lower if
    // the user moves the phone rapidly
    private var accelX: Float = 0.toFloat()
    private var accelY: Float = 0.toFloat()

    @Volatile private var lastTimeMs: Long = -1

    val LOCK = Any()

    //private val vibratorRef = AtomicReference<Vibrator>()

    var onTouchAllSideListener: OnTouchAllSideListener? = null


    fun setAccel(ax: Float, ay: Float) {
        synchronized(LOCK) {
            this.accelX = ax
            this.accelY = ay
        }
    }

    fun setSize(width: Int, height: Int) {
        synchronized(LOCK) {
            this.pixelWidth = width
            this.pixelHeight = height
        }
    }

    /**
     * Call this to move the ball to a particular location on the screen. This
     * resets the velocity to zero, but the acceleration doesn't change so
     * the ball should start falling shortly.
     */
    fun moveBall(ballX: Int, ballY: Int) {
        synchronized(LOCK) {
            this.ballPixelX = ballX.toFloat()
            this.ballPixelY = ballY.toFloat()
            velocityX = 0f
            velocityY = 0f
        }
    }

    fun updatePhysics() {
        // copy everything to local vars (hence the 'l' prefix)
        synchronized(LOCK) {
            val lWidth = pixelWidth.toFloat()
            val lHeight = pixelHeight.toFloat()
            var lBallX = ballPixelX
            var lBallY = ballPixelY
            var lVx = velocityX
            var lVy = velocityY
            val lAx = accelX
            val lAy = -accelY

            if (lWidth <= 0 || lHeight <= 0) {
                // invalid width and height, nothing to do until the GUI comes up
                return
            }
            val curTime = System.currentTimeMillis()
            if (lastTimeMs < 0) {
                lastTimeMs = curTime
                return
            }

            val elapsedMs = curTime - lastTimeMs
            lastTimeMs = curTime

            // update the velocity
            // (divide by 1000 to convert ms to seconds)
            // end result is meters / second
            lVx += elapsedMs * lAx / 1000 * pixelsPerMeter
            lVy += elapsedMs * lAy / 1000 * pixelsPerMeter

            // update the position
            // (velocity is meters/sec, so divide by 1000 again)
            lBallX += lVx * elapsedMs / 1000 * pixelsPerMeter
            lBallY += lVy * elapsedMs / 1000 * pixelsPerMeter

            var bouncedX = false
            var bouncedY = false

            //Log.d("BALL","BALL X "+lBallX + " BALL Y "+lBallY + " ballRadius "+ballRadius);
            if (lBallY < 0) {
                lBallY = 0f
                lVy = -lVy * rebound
                bouncedY = true
                //Log.d("BALL","Y 1st IF");
            } else if (lBallY + ballRadius > lHeight) {
                lBallY = lHeight - ballRadius
                lVy = -lVy * rebound
                bouncedY = true
                //Log.d("BALL","Y 2nd IF");
            }
            if (bouncedY && Math.abs(lVy) < STOP_BOUNCING_VELOCITY) {
                lVy = 0f
                bouncedY = false
            }

            if (lBallX < 0) {
                lBallX = 0f
                lVx = -lVx * rebound
                bouncedX = true

                //Log.d("BALL","X 1st IF");
            } else if (lBallX + ballRadius > lWidth) {
                lBallX = lWidth - ballRadius
                lVx = -lVx * rebound
                bouncedX = true
                //Log.d("BALL","X 1st IF");
            }
            if (bouncedX && Math.abs(lVx) < STOP_BOUNCING_VELOCITY) {
                lVx = 0f
                bouncedX = false
            }
            if (!initialDrawOffset) {
                initialDrawOffset = true
                lBallX = (lWidth * 0.20).toFloat()
                lBallY = (lHeight * 0.20).toFloat()
            }

            if (lBallX <= 0 && !leftEdge) {
                leftEdge = true
                Log.d("Ball", "left edge")
            }
            if (lBallY <= 0 && !topEdge) {
                topEdge = true
                Log.d("Ball", "top edge")

            }
            if (lBallY + ballRadius >= lHeight && !bottomEdge) {
                bottomEdge = true
                Log.d("Ball", "bottom edge")

            }
            if (lBallX + ballRadius >= lWidth && !rightEdge) {
                rightEdge = true
                Log.d("Ball", "right edge")
            }

            if (topEdge && bottomEdge && leftEdge && rightEdge && !isTouchAllCorners) {
                onTouchAllSideListener?.onDone()
                Log.d("Ball ", "Done")
                isTouchAllCorners = true
            }
            // safely copy local vars back to object fields
            synchronized(LOCK) {

                ballPixelX = lBallX
                ballPixelY = lBallY


                velocityX = lVx
                velocityY = lVy
            }
        }





    }

    interface OnTouchAllSideListener {
        fun onDone()
    }

    companion object {

        /**
         * When the ball hits an edge, multiply the velocity by the rebound.
         * A value of 1.0 means the ball bounces with 100% efficiency. Lower
         * numbers simulate balls that don't bounce very much.
         */
        private val rebound = 0.1f

        // if the ball bounces and the velocity is less than this constant,
        // stop bouncing.
        private val STOP_BOUNCING_VELOCITY = 1.5f
    }
}
