package fun.aloyenz.stfr.reactor.physics;

import java.util.ArrayList;
import java.util.List;

/** One sustained source plus independent, bounded transient impacts for one reactor. */
public final class StfrShakeMixer {
	private Layer background;
	private final List<Layer> impacts = new ArrayList<>();

	public void add(long now, int duration, int frequency, int magnitude, boolean sustained) {
		Layer layer = new Layer(now, Math.max(1, duration), frequency, magnitude, sustained);
		if (sustained) background = layer;
		else {
			prune(now);
			if (impacts.size() >= 8) impacts.remove(0);
			impacts.add(layer);
		}
	}

	public void fadeBackground(long now, int duration) {
		if (background != null && !background.expired(now)) background.fade(now, duration);
	}

	public boolean prune(double now) {
		if (background != null && background.expired(now)) background = null;
		impacts.removeIf(layer -> layer.expired(now));
		return background == null && impacts.isEmpty();
	}

	public double backgroundAmplitude(double now) {
		return background == null ? 0 : background.amplitude(now);
	}

	public Sample sample(double now, double gain) {
		Sample sample = new Sample();
		if (background != null) sample.add(background, now, gain);
		for (Layer layer : impacts) {
			sample.add(layer, now, gain);
			sample.impact = Math.max(sample.impact, layer.amplitude(now) * gain);
		}
		return sample;
	}

	public static final class Sample {
		public double pitch, yaw, roll, impact;
		private void add(Layer layer, double now, double gain) {
			double amplitude = layer.amplitude(now) * gain;
			double phase = (now-layer.start)*Math.max(1,layer.frequency)*.03;
			pitch += Math.sin(phase)*amplitude;
			yaw += Math.sin(phase*1.37)*amplitude;
			roll += Math.sin(phase*.73)*amplitude*.5;
		}
	}

	private static final class Layer {
		final long start;
		final int duration, frequency;
		final double magnitude;
		final boolean sustained;
		long fadeStart;
		int fadeDuration;
		double fadeAmplitude;
		Layer(long start,int duration,int frequency,int magnitude,boolean sustained) {
			this.start=start; this.duration=duration; this.frequency=frequency;
			this.magnitude=Math.min(5,Math.max(0,magnitude)*.1); this.sustained=sustained;
		}
		boolean expired(double now) {
			return now >= (fadeDuration>0 ? fadeStart+fadeDuration : start+duration);
		}
		double amplitude(double now) {
			if (expired(now)) return 0;
			if (fadeDuration>0) {
				double t=Math.max(0,Math.min(1,(now-fadeStart)/fadeDuration));
				return fadeAmplitude*(1-t*t*(3-2*t));
			}
			return sustained ? magnitude : magnitude*Math.max(0,1-(now-start)/duration);
		}
		void fade(long now,int duration) {
			fadeAmplitude=amplitude(now); fadeStart=now; fadeDuration=Math.max(1,duration);
		}
	}
}
