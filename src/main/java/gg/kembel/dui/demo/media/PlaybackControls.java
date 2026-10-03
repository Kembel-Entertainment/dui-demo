package gg.kembel.dui.demo.media;

import gg.kembel.dui.core.video.SurfaceInput;

/** Consumer policy: one adjustment per movement-key press, even when other held keys change. */
final class PlaybackControls {
  record Adjustment(double seekSeconds, int speedSteps) {}
  private boolean left, right, forward, backward;
  private static final double[] SPEEDS = {.25, .5, .75, 1, 1.25, 1.5, 2, 3, 4};
  Adjustment accept(SurfaceInput input) {
    if (input.type() != SurfaceInput.Type.STATE) return new Adjustment(0, 0);
    double seek = input.left() == input.right() ? 0 : input.left() && !left ? -5 : input.right() && !right ? 5 : 0;
    int speed = input.forward() == input.backward() ? 0 : input.forward() && !forward ? 1 : input.backward() && !backward ? -1 : 0;
    left = input.left(); right = input.right(); forward = input.forward(); backward = input.backward();
    return new Adjustment(seek, speed);
  }
  static double speed(double current, int steps) {
    int closest = 0;
    for (int i = 1; i < SPEEDS.length; i++)
      if (Math.abs(SPEEDS[i] - current) < Math.abs(SPEEDS[closest] - current)) closest = i;
    return SPEEDS[Math.max(0, Math.min(SPEEDS.length - 1, closest + steps))];
  }
}
