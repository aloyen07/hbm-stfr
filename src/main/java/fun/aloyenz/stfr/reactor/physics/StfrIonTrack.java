package fun.aloyenz.stfr.reactor.physics;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiFunction;
import net.minecraft.util.math.Vec3d;

/** Pure flight/history model: no entities are queried and no physical damage is applied. */
public final class StfrIonTrack {
	public static final int DELAY = 2, TRAIL_LIFE = 32;
	public static final class Sample {
		public final Vec3d pos;
		public final double time;
		Sample(Vec3d pos, double time) { this.pos=pos; this.time=time; }
	}
	public final Deque<Sample> samples = new ArrayDeque<>();
	public final Vec3d center;
	public Vec3d position, previous, velocity;
	public boolean flying = true;
	private int age;
	public StfrIonTrack(Vec3d center, Vec3d direction) {
		this.center=center;
		position=previous=center;
		velocity=direction.normalize().scale(.72);
	}
	public void tick(double time, BiFunction<Vec3d, Vec3d, Vec3d> collision) {
		previous=position;
		if (flying) {
			Vec3d radial=position.subtract(center);
			double radius=Math.max(1,Math.sqrt(radial.x*radial.x+radial.z*radial.z));
			velocity=velocity.add(-radial.z/radius*.045,0,radial.x/radius*.045);
			if (velocity.lengthSquared() > 1) velocity=velocity.normalize();
			Vec3d end=position.add(velocity), hit=collision.apply(position,end);
			if (hit != null) { end=hit; flying=false; }
			for (int i=1;i<=3;i++) samples.addLast(new Sample(position.add(end.subtract(position).scale(i/3D)),time-1+i/3D));
			position=end;
			if (++age>=45) flying=false;
		}
		while (!samples.isEmpty() && time-samples.getFirst().time > TRAIL_LIFE) samples.removeFirst();
	}
	public static float alpha(double age) {
		if (age < DELAY || age >= TRAIL_LIFE) return 0;
		double fade=(age-DELAY)/(TRAIL_LIFE-DELAY);
		return (float) (Math.min(1,(age-DELAY)/2D)*Math.pow(1-fade,2));
	}
	public boolean expired() { return !flying && samples.isEmpty(); }
}
