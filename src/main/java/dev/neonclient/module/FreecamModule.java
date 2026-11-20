package dev.neonclient.module;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Freecam client-side: il corpo resta fermo, la telecamera vola libera e attraversa i blocchi.
 * Nessun pacchetto viene inviato al server per la camera.
 */
public class FreecamModule extends Module {
    public final NumberSetting speed = new NumberSetting("Speed", 1, 50, 10, 0.5);

    public double x, y, z;
    public float yaw, pitch;

    private long last;
    private LocalPlayer owner;
    private ClientInput savedInput;
    private final ClientInput frozenInput = new ClientInput();
    private CameraType savedType;
    private boolean savedCull = true;

    public FreecamModule() {
        super("Freecam", Category.RENDER);
        settings.add(speed);
    }

    /** True quando la camera libera sta davvero controllando la vista. */
    public boolean isActive() {
        return enabled && owner != null;
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            enabled = false;
            return;
        }
        owner = mc.player;
        Vec3 eye = owner.getEyePosition();
        x = eye.x;
        y = eye.y;
        z = eye.z;
        yaw = owner.getYRot();
        pitch = owner.getXRot();

        // Il corpo non deve piu' muoversi: sostituiamo l'input con uno vuoto.
        savedInput = owner.input;
        owner.input = frozenInput;

        // Terza persona: il corpo viene disegnato e la mano in prima persona sparisce.
        savedType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        // Senza culling delle caverne vengono disegnate anche le sezioni sotto terra / dietro i muri.
        savedCull = mc.smartCull;
        mc.smartCull = false;
        last = System.nanoTime();
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        mc.smartCull = savedCull;
        if (owner != null && mc.player == owner && owner.input == frozenInput && savedInput != null) {
            owner.input = savedInput;
        }
        if (savedType != null) {
            mc.options.setCameraType(savedType);
        }
        owner = null;
        savedInput = null;
        savedType = null;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player != owner) {
            setEnabled(false);
            return;
        }
        if (mc.player.input != frozenInput) mc.player.input = frozenInput;
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    /** Chiamato a ogni frame dal mixin della Camera: movimento fluido, indipendente dai tick. */
    public void frameUpdate() {
        long now = System.nanoTime();
        double dt = Math.min((now - last) / 1_000_000_000.0, 0.1);
        last = now;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        Options o = mc.options;
        double f = 0, s = 0, u = 0;
        if (o.keyUp.isDown()) f += 1;
        if (o.keyDown.isDown()) f -= 1;
        if (o.keyRight.isDown()) s += 1;
        if (o.keyLeft.isDown()) s -= 1;
        if (o.keyJump.isDown()) u += 1;
        if (o.keyShift.isDown()) u -= 1;

        // Come la spectator: W/S avanti/indietro sul piano orizzontale (ignora lo sguardo in alto/basso),
        // A/D di lato, Space/Shift su/giu'.
        double yr = Math.toRadians(yaw);
        double fx = -Math.sin(yr);
        double fz = Math.cos(yr);
        double rx = -Math.cos(yr);
        double rz = -Math.sin(yr);
        double len = Math.sqrt(f * f + s * s);
        if (len > 1) {
            f /= len;
            s /= len;
        }

        double d = speed.value * dt;
        x += (fx * f + rx * s) * d;
        y += u * d;
        z += (fz * f + rz * s) * d;
    }

    /** Movimento del mouse (gia' scalato come in vanilla da Entity#turn). */
    public void rotate(double yawDelta, double pitchDelta) {
        yaw += (float) (yawDelta * 0.15);
        pitch += (float) (pitchDelta * 0.15);
        pitch = Math.max(-90f, Math.min(90f, pitch));
    }
}
