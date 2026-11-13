package com.example.engine

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.CatalogCharacter
import com.example.data.model.CatalogWeapon
import com.example.data.model.LocalPlayerSettings
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val VERTEX_SHADER = """
uniform mat4 uMvpMatrix;
uniform mat4 uModelMatrix;
attribute vec3 aPosition;
attribute vec3 aNormal;
varying vec3 vWorldNormal;
varying vec3 vWorldPos;
void main() {
    vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
    vWorldPos = worldPos.xyz;
    vWorldNormal = mat3(uModelMatrix) * aNormal;
    gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
}
"""

private const val FRAGMENT_SHADER = """
precision mediump float;
uniform vec4 uColor;
uniform vec3 uSunDir;
uniform vec3 uCameraPos;
uniform vec3 uFogColor;
uniform float uFogDensity;
uniform float uUnlit;
varying vec3 vWorldNormal;
varying vec3 vWorldPos;
void main() {
    vec3 norm = normalize(vWorldNormal);
    float diff = max(dot(norm, normalize(uSunDir)), 0.0);
    vec3 viewDir = normalize(uCameraPos - vWorldPos);
    vec3 halfDir = normalize(normalize(uSunDir) + viewDir);
    float spec = pow(max(dot(norm, halfDir), 0.0), 24.0) * 0.28;
    vec3 litColor = mix(uColor.rgb * (0.36 + 0.68 * diff) + vec3(spec), uColor.rgb, uUnlit);
    float dist = length(uCameraPos - vWorldPos);
    float fogFactor = clamp(exp(-dist * uFogDensity), 0.0, 1.0);
    vec3 finalRgb = mix(uFogColor, litColor, fogFactor);
    gl_FragColor = vec4(finalRgb, uColor.a);
}
"""

private class PrimitiveMeshes {
  val cubeBuffer: FloatBuffer
  val cubeVertexCount = 36

  val cylinderWallBuffer: FloatBuffer
  val cylinderWallVertexCount: Int

  val discBuffer: FloatBuffer
  val discVertexCount: Int

  init {
    // 36 vertices: x,y,z, nx,ny,nz
    val c = floatArrayOf(
      // Front (+Z)
      -0.5f,-0.5f, 0.5f, 0f,0f,1f,  0.5f,-0.5f, 0.5f, 0f,0f,1f,  0.5f, 0.5f, 0.5f, 0f,0f,1f,
      -0.5f,-0.5f, 0.5f, 0f,0f,1f,  0.5f, 0.5f, 0.5f, 0f,0f,1f, -0.5f, 0.5f, 0.5f, 0f,0f,1f,
      // Back (-Z)
       0.5f,-0.5f,-0.5f, 0f,0f,-1f, -0.5f,-0.5f,-0.5f, 0f,0f,-1f, -0.5f, 0.5f,-0.5f, 0f,0f,-1f,
       0.5f,-0.5f,-0.5f, 0f,0f,-1f, -0.5f, 0.5f,-0.5f, 0f,0f,-1f,  0.5f, 0.5f,-0.5f, 0f,0f,-1f,
      // Left (-X)
      -0.5f,-0.5f,-0.5f,-1f,0f,0f, -0.5f,-0.5f, 0.5f,-1f,0f,0f, -0.5f, 0.5f, 0.5f,-1f,0f,0f,
      -0.5f,-0.5f,-0.5f,-1f,0f,0f, -0.5f, 0.5f, 0.5f,-1f,0f,0f, -0.5f, 0.5f,-0.5f,-1f,0f,0f,
      // Right (+X)
       0.5f,-0.5f, 0.5f, 1f,0f,0f,  0.5f,-0.5f,-0.5f, 1f,0f,0f,  0.5f, 0.5f,-0.5f, 1f,0f,0f,
       0.5f,-0.5f, 0.5f, 1f,0f,0f,  0.5f, 0.5f,-0.5f, 1f,0f,0f,  0.5f, 0.5f, 0.5f, 1f,0f,0f,
      // Top (+Y)
      -0.5f, 0.5f, 0.5f, 0f,1f,0f,  0.5f, 0.5f, 0.5f, 0f,1f,0f,  0.5f, 0.5f,-0.5f, 0f,1f,0f,
      -0.5f, 0.5f, 0.5f, 0f,1f,0f,  0.5f, 0.5f,-0.5f, 0f,1f,0f, -0.5f, 0.5f,-0.5f, 0f,1f,0f,
      // Bottom (-Y)
      -0.5f,-0.5f,-0.5f, 0f,-1f,0f,  0.5f,-0.5f,-0.5f, 0f,-1f,0f,  0.5f,-0.5f, 0.5f, 0f,-1f,0f,
      -0.5f,-0.5f,-0.5f, 0f,-1f,0f,  0.5f,-0.5f, 0.5f, 0f,-1f,0f, -0.5f,-0.5f, 0.5f, 0f,-1f,0f,
    )
    cubeBuffer = toFloatBuffer(c)

    val segs = 32
    val cyl = FloatArray(segs * 6 * 6)
    var idx = 0
    for (i in 0 until segs) {
      val a0 = (i.toFloat() / segs) * 2f * PI.toFloat()
      val a1 = ((i + 1).toFloat() / segs) * 2f * PI.toFloat()
      val x0 = cos(a0); val z0 = sin(a0)
      val x1 = cos(a1); val z1 = sin(a1)
      fun put(x: Float, y: Float, z: Float, nx: Float, nz: Float) {
        cyl[idx++] = x; cyl[idx++] = y; cyl[idx++] = z
        cyl[idx++] = nx; cyl[idx++] = 0f; cyl[idx++] = nz
      }
      put(x0, 0f, z0, x0, z0); put(x1, 0f, z1, x1, z1); put(x1, 1f, z1, x1, z1)
      put(x0, 0f, z0, x0, z0); put(x1, 1f, z1, x1, z1); put(x0, 1f, z0, x0, z0)
    }
    cylinderWallBuffer = toFloatBuffer(cyl)
    cylinderWallVertexCount = segs * 6

    val disc = FloatArray(segs * 3 * 6)
    var dIdx = 0
    for (i in 0 until segs) {
      val a0 = (i.toFloat() / segs) * 2f * PI.toFloat()
      val a1 = ((i + 1).toFloat() / segs) * 2f * PI.toFloat()
      val x0 = cos(a0); val z0 = sin(a0)
      val x1 = cos(a1); val z1 = sin(a1)
      fun putD(x: Float, z: Float) {
        disc[dIdx++] = x; disc[dIdx++] = 0f; disc[dIdx++] = z
        disc[dIdx++] = 0f; disc[dIdx++] = 1f; disc[dIdx++] = 0f
      }
      putD(0f, 0f); putD(x1, z1); putD(x0, z0)
    }
    discBuffer = toFloatBuffer(disc)
    discVertexCount = segs * 3
  }

  private fun toFloatBuffer(arr: FloatArray): FloatBuffer =
    ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
      put(arr)
      position(0)
    }
}

class BattleRoyaleGLRenderer(
  private val isLobbyShowcase: Boolean,
  private val simulationProvider: () -> BattleSimulation?,
  private val showcaseCharacterProvider: () -> CatalogCharacter,
  private val showcaseWeaponProvider: () -> CatalogWeapon?,
  private val settingsProvider: () -> LocalPlayerSettings,
  private val onHudSnapshot: (HudSnapshot) -> Unit,
) : GLSurfaceView.Renderer {

  private var program = 0
  private var posHandle = 0
  private var normHandle = 0
  private var mvpHandle = 0
  private var modelHandle = 0
  private var colorHandle = 0
  private var sunDirHandle = 0
  private var camPosHandle = 0
  private var fogColorHandle = 0
  private var fogDensityHandle = 0
  private var unlitHandle = 0
  private lateinit var meshes: PrimitiveMeshes

  private val viewMatrix = FloatArray(16)
  private val projMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)
  private val vpMatrix = FloatArray(16)

  private var lastFrameNanos = System.nanoTime()
  private var hudAccumSec = 0f
  @Volatile var showcaseOrbitYawDeg = 20f
  private var showcaseIdleTime = 0f

  override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

    val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
    program = GLES20.glCreateProgram().also {
      GLES20.glAttachShader(it, vs)
      GLES20.glAttachShader(it, fs)
      GLES20.glLinkProgram(it)
    }
    posHandle = GLES20.glGetAttribLocation(program, "aPosition")
    normHandle = GLES20.glGetAttribLocation(program, "aNormal")
    mvpHandle = GLES20.glGetUniformLocation(program, "uMvpMatrix")
    modelHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
    colorHandle = GLES20.glGetUniformLocation(program, "uColor")
    sunDirHandle = GLES20.glGetUniformLocation(program, "uSunDir")
    camPosHandle = GLES20.glGetUniformLocation(program, "uCameraPos")
    fogColorHandle = GLES20.glGetUniformLocation(program, "uFogColor")
    fogDensityHandle = GLES20.glGetUniformLocation(program, "uFogDensity")
    unlitHandle = GLES20.glGetUniformLocation(program, "uUnlit")

    meshes = PrimitiveMeshes()
    lastFrameNanos = System.nanoTime()
  }

  override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
    val scale = settingsProvider().graphicsTier.renderScale
    GLES20.glViewport(0, 0, width, height)
    val ratio = width.toFloat() / height.coerceAtLeast(1).toFloat()
    val fov = if (isLobbyShowcase) 42f else 58f
    Matrix.perspectiveM(projMatrix, 0, fov * (1f + (1f - scale) * 0.05f), ratio, 0.25f, 380f)
  }

  override fun onDrawFrame(unused: GL10?) {
    val now = System.nanoTime()
    val dt = ((now - lastFrameNanos) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
    lastFrameNanos = now

    GLES20.glUseProgram(program)
    if (isLobbyShowcase) {
      renderLobbyShowcase(dt)
    } else {
      renderBattleMatch(dt)
    }
  }

  private fun renderLobbyShowcase(dt: Float) {
    showcaseIdleTime += dt
    val skyR = 0.055f; val skyG = 0.08f; val skyB = 0.13f
    GLES20.glClearColor(skyR, skyG, skyB, 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

    val camDist = 4.6f
    val yawRad = Math.toRadians(showcaseOrbitYawDeg.toDouble())
    val camX = (sin(yawRad) * camDist).toFloat()
    val camZ = (cos(yawRad) * camDist).toFloat()
    val camY = 1.75f
    Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, 0f, 1.05f, 0f, 0f, 1f, 0f)
    Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

    GLES20.glUniform3f(sunDirHandle, 0.55f, 0.85f, 0.45f)
    GLES20.glUniform3f(camPosHandle, camX, camY, camZ)
    GLES20.glUniform3f(fogColorHandle, skyR, skyG, skyB)
    GLES20.glUniform1f(fogDensityHandle, 0.04f)

    // Hangar floor & glowing holographic turntable disc
    drawDisc(0f, -0.02f, 0f, 14f, 0.08f, 0.11f, 0.16f, 1f)
    drawDisc(0f, 0.0f, 0f, 1.45f, 0.14f, 0.19f, 0.28f, 1f)
    val char = showcaseCharacterProvider()
    val pRgb = BattleSimulation.hexToRgb(char.primaryColorHex)
    val aRgb = BattleSimulation.hexToRgb(char.accentColorHex)
    drawDisc(0f, 0.01f, 0f, 1.18f, aRgb[0], aRgb[1], aRgb[2], 0.35f, unlit = true)

    // Background tactical pillars
    for (i in -2..2) {
      if (i == 0) continue
      drawCube(i * 2.4f, 2.2f, -4.5f, 0.5f, 4.4f, 0.5f, 0f, 0.12f, 0.16f, 0.23f, 1f)
    }

    // Articulated 3D Character on Turntable
    val breath = sin(showcaseIdleTime * 2.2f) * 0.025f
    val weapon = showcaseWeaponProvider()
    val wRgb = weapon?.let { BattleSimulation.hexToRgb(it.colorHex) } ?: floatArrayOf(1f, 0.55f, 0f)
    drawHumanoidCharacter(
      x = 0f,
      y = 0.02f + breath,
      z = 0f,
      yawDeg = 180f,
      walkCycle = 0f,
      isCrouching = false,
      isParachuting = false,
      primaryRgb = pRgb,
      accentRgb = aRgb,
      weaponRgb = wRgb,
      drawShadow = true,
    )
  }

  private fun renderBattleMatch(dt: Float) {
    val sim = simulationProvider() ?: return
    val settings = settingsProvider()
    sim.settings = settings
    sim.step(dt)

    hudAccumSec += dt
    if (hudAccumSec >= 0.055f) {
      hudAccumSec = 0f
      onHudSnapshot(sim.snapshotHud())
    }

    val skyRgb = BattleSimulation.hexToRgb(sim.map.skyColorHex)
    val groundRgb = BattleSimulation.hexToRgb(sim.map.groundColorHex)
    GLES20.glClearColor(skyRgb[0], skyRgb[1], skyRgb[2], 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

    val focusTarget = sim.getSpectateTarget()
    val yawDeg = if (focusTarget.id == sim.localPlayer.id) sim.cameraYawDeg else focusTarget.yawDeg
    val pitchDeg = if (focusTarget.id == sim.localPlayer.id) sim.cameraPitchDeg else 18f
    val yawRad = Math.toRadians(yawDeg.toDouble())
    val pitchRad = Math.toRadians(pitchDeg.toDouble())

    val camDist = when {
      focusTarget.isParachuting -> 8.5f
      focusTarget.vehicleId.isNotEmpty() -> 7.8f
      sim.isAiming -> 2.7f
      else -> 5.0f
    }
    val lookHeight = if (focusTarget.isCrouching) 1.05f else 1.55f
    val shoulderOffsetX = (cos(yawRad) * 0.55).toFloat()
    val shoulderOffsetZ = (sin(yawRad) * 0.55).toFloat()

    val camX = focusTarget.x + shoulderOffsetX - (sin(yawRad) * cos(pitchRad)).toFloat() * camDist
    val camY = (focusTarget.y + lookHeight + sin(pitchRad).toFloat() * camDist).coerceAtLeast(0.8f)
    val camZ = focusTarget.z + shoulderOffsetZ + (cos(yawRad) * cos(pitchRad)).toFloat() * camDist

    val lookTargetX = focusTarget.x + shoulderOffsetX + sin(yawRad).toFloat() * 6.0f
    val lookTargetY = focusTarget.y + lookHeight - sin(pitchRad).toFloat() * 2.2f
    val lookTargetZ = focusTarget.z + shoulderOffsetZ - cos(yawRad).toFloat() * 6.0f

    Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookTargetX, lookTargetY, lookTargetZ, 0f, 1f, 0f)
    Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

    GLES20.glUniform3f(sunDirHandle, 0.45f, 0.85f, 0.35f)
    GLES20.glUniform3f(camPosHandle, camX, camY, camZ)
    GLES20.glUniform3f(fogColorHandle, skyRgb[0], skyRgb[1], skyRgb[2])
    val fogDensity = when (settings.graphicsTier.shadowDetail) {
      0 -> 0.011f
      1 -> 0.0085f
      else -> 0.0062f
    }
    GLES20.glUniform1f(fogDensityHandle, fogDensity)

    // 1. Terrain Floor + Ganga River + Bridges + Roads + Airfield Runway
    drawCube(0f, -0.5f, 0f, 260f, 1f, 260f, 0f, groundRgb[0], groundRgb[1], groundRgb[2], 1f)
    // Winding River strip along X=0
    drawCube(0f, 0.02f, 0f, 16f, 0.04f, 250f, 0f, 0.08f, 0.32f, 0.52f, 1f)
    // Asphalt Roads
    drawCube(0f, 0.03f, -40f, 190f, 0.05f, 6.5f, 0f, 0.16f, 0.18f, 0.21f, 1f)
    drawCube(0f, 0.03f, 40f, 190f, 0.05f, 6.5f, 0f, 0.16f, 0.18f, 0.21f, 1f)
    drawCube(24f, 0.03f, 0f, 6.5f, 0.05f, 190f, 0f, 0.16f, 0.18f, 0.21f, 1f)
    // Two elevated bridges over river
    drawCube(0f, 0.35f, -40f, 20f, 0.45f, 7.2f, 0f, 0.45f, 0.48f, 0.52f, 1f)
    drawCube(0f, 0.35f, 40f, 20f, 0.45f, 7.2f, 0f, 0.45f, 0.48f, 0.52f, 1f)
    // Military Airfield Runway (SE)
    drawCube(42f, 0.04f, 44f, 44f, 0.06f, 10f, 0f, 0.22f, 0.24f, 0.27f, 1f)

    // 2. 3D Buildings & Obstacles
    for (o in sim.obstacles) {
      val cx = (o.minX + o.maxX) * 0.5f
      val cz = (o.minZ + o.maxZ) * 0.5f
      val sx = o.maxX - o.minX
      val sz = o.maxZ - o.minZ
      drawCube(cx, o.height * 0.5f, cz, sx, o.height, sz, 0f, o.colorRgb[0], o.colorRgb[1], o.colorRgb[2], 1f)
      // Roof trim
      drawCube(cx, o.height + 0.15f, cz, sx + 0.35f, 0.3f, sz + 0.35f, 0f, o.colorRgb[0] * 0.7f, o.colorRgb[1] * 0.7f, o.colorRgb[2] * 0.7f, 1f)
    }

    // 3. Trees
    for (t in sim.trees) {
      drawCube(t.x, 1.8f * t.scale, t.z, 0.75f * t.scale, 3.6f * t.scale, 0.75f * t.scale, 0f, 0.36f, 0.22f, 0.12f, 1f)
      drawCube(t.x, 4.2f * t.scale, t.z, 3.2f * t.scale, 2.8f * t.scale, 3.2f * t.scale, 15f, 0.13f, 0.45f, 0.18f, 1f)
    }

    // 4. Loot Crates (with bobbing animation)
    val bob = sin(sim.snapshotHud().matchElapsedSec * 3.0f + showcaseIdleTime) * 0.12f
    showcaseIdleTime += dt
    for (loot in sim.lootDrops) {
      if (loot.collected) continue
      val (r, g, b) = when (loot.kind) {
        LootKind.WEAPON -> Triple(1.0f, 0.55f, 0.05f)
        LootKind.ARMOR -> Triple(0.1f, 0.75f, 1.0f)
        LootKind.MEDKIT -> Triple(0.1f, 0.95f, 0.45f)
        LootKind.AMMO -> Triple(1.0f, 0.85f, 0.15f)
      }
      drawCube(loot.x, 0.45f + bob * 0.5f, loot.z, 0.7f, 0.55f, 0.7f, showcaseIdleTime * 45f, r, g, b, 1f)
    }

    // 5. Drivable 3D Vehicles
    for (v in sim.vehicles) {
      if (v.hp <= 0) continue
      drawVehicle(v, settings.shadowsEnabled)
    }

    // 6. Combatants (Local Player, Remote Players, AI Bots)
    for (c in sim.combatants) {
      if (!c.isAlive) continue
      if (c.vehicleId.isNotEmpty()) continue // Rendered inside vehicle
      val w = sim.weaponsCatalog.firstOrNull { it.id == c.weaponId }
      val wRgb = w?.let { BattleSimulation.hexToRgb(it.colorHex) } ?: floatArrayOf(1f, 0.6f, 0.1f)
      drawHumanoidCharacter(
        x = c.x,
        y = c.y,
        z = c.z,
        yawDeg = c.yawDeg,
        walkCycle = c.walkCycle,
        isCrouching = c.isCrouching,
        isParachuting = c.isParachuting,
        primaryRgb = c.primaryRgb,
        accentRgb = c.accentRgb,
        weaponRgb = wRgb,
        drawShadow = settings.shadowsEnabled,
      )
    }

    // 7. Bullet Tracers
    for (tr in sim.tracers) {
      val mx = (tr.startX + tr.endX) * 0.5f
      val my = (tr.startY + tr.endY) * 0.5f
      val mz = (tr.startZ + tr.endZ) * 0.5f
      val dx = tr.endX - tr.startX
      val dz = tr.endZ - tr.startZ
      val len = kotlin.math.hypot(dx, dz).coerceAtLeast(0.5f)
      val yaw = Math.toDegrees(kotlin.math.atan2(dx.toDouble(), dz.toDouble())).toFloat()
      drawCube(mx, my, mz, 0.08f, 0.08f, len, yaw, 1f, 0.88f, 0.22f, 0.9f, unlit = true)
    }

    // 8. Shrinking Translucent Blue Zone Cylinder Wall
    val hud = sim.snapshotHud()
    GLES20.glDepthMask(false)
    drawCylinderWall(hud.zoneCenterX, 0f, hud.zoneCenterZ, hud.zoneRadius, 48f, 0.12f, 0.52f, 1.0f, 0.28f)
    GLES20.glDepthMask(true)
  }

  private fun drawHumanoidCharacter(
    x: Float,
    y: Float,
    z: Float,
    yawDeg: Float,
    walkCycle: Float,
    isCrouching: Boolean,
    isParachuting: Boolean,
    primaryRgb: FloatArray,
    accentRgb: FloatArray,
    weaponRgb: FloatArray,
    drawShadow: Boolean,
  ) {
    if (drawShadow && y < 25f) {
      drawDisc(x, 0.05f, z, 0.65f, 0f, 0f, 0f, 0.35f, unlit = true)
    }

    val crouchDrop = if (isCrouching) -0.35f else 0f
    val legSwing = sin(walkCycle) * 28f
    val armSwing = -sin(walkCycle) * 20f
    val yawRad = Math.toRadians(yawDeg.toDouble())
    val fwdX = sin(yawRad).toFloat()
    val fwdZ = -cos(yawRad).toFloat()
    val rightX = cos(yawRad).toFloat()
    val rightZ = sin(yawRad).toFloat()

    // Legs
    val legH = if (isCrouching) 0.45f else 0.72f
    drawCube(x - rightX * 0.16f, y + legH * 0.5f, z - rightZ * 0.16f, 0.22f, legH, 0.24f, -yawDeg + legSwing * 0.2f, 0.14f, 0.16f, 0.20f, 1f)
    drawCube(x + rightX * 0.16f, y + legH * 0.5f, z + rightZ * 0.16f, 0.22f, legH, 0.24f, -yawDeg - legSwing * 0.2f, 0.14f, 0.16f, 0.20f, 1f)

    // Torso & Armor Vest
    val torsoY = y + 1.08f + crouchDrop
    drawCube(x, torsoY, z, 0.56f, 0.68f, 0.32f, -yawDeg, primaryRgb[0], primaryRgb[1], primaryRgb[2], 1f)
    drawCube(x, torsoY + 0.04f, z, 0.60f, 0.38f, 0.36f, -yawDeg, accentRgb[0], accentRgb[1], accentRgb[2], 1f)

    // Head & Tactical Helmet/Hair
    val headY = y + 1.62f + crouchDrop
    drawCube(x, headY, z, 0.34f, 0.34f, 0.34f, -yawDeg, 0.85f, 0.68f, 0.54f, 1f)
    drawCube(x, headY + 0.16f, z, 0.38f, 0.14f, 0.38f, -yawDeg, 0.15f, 0.18f, 0.24f, 1f)

    // Arms + Equipped 3D Weapon in right hand
    drawCube(x - rightX * 0.38f, torsoY + 0.04f, z - rightZ * 0.38f, 0.18f, 0.56f, 0.18f, -yawDeg + armSwing * 0.15f, primaryRgb[0] * 0.9f, primaryRgb[1] * 0.9f, primaryRgb[2] * 0.9f, 1f)
    drawCube(x + rightX * 0.38f + fwdX * 0.12f, torsoY + 0.06f, z + rightZ * 0.38f + fwdZ * 0.12f, 0.18f, 0.52f, 0.18f, -yawDeg, primaryRgb[0] * 0.9f, primaryRgb[1] * 0.9f, primaryRgb[2] * 0.9f, 1f)

    // 3D Weapon Barrel
    drawCube(
      x + rightX * 0.36f + fwdX * 0.42f,
      torsoY + 0.12f,
      z + rightZ * 0.36f + fwdZ * 0.42f,
      0.10f,
      0.14f,
      0.78f,
      -yawDeg,
      weaponRgb[0],
      weaponRgb[1],
      weaponRgb[2],
      1f,
    )

    // Parachute Canopy when descending
    if (isParachuting) {
      drawCube(x, y + 3.4f, z, 2.8f, 0.35f, 1.7f, -yawDeg, 1.0f, 0.45f, 0.05f, 0.95f)
      drawCube(x - rightX * 0.65f, y + 2.35f, z - rightZ * 0.65f, 0.04f, 1.9f, 0.04f, -yawDeg, 0.9f, 0.9f, 0.9f, 1f)
      drawCube(x + rightX * 0.65f, y + 2.35f, z + rightZ * 0.65f, 0.04f, 1.9f, 0.04f, -yawDeg, 0.9f, 0.9f, 0.9f, 1f)
    }
  }

  private fun drawVehicle(v: VehicleEntity, drawShadow: Boolean) {
    if (drawShadow) {
      drawDisc(v.x, 0.05f, v.z, 1.8f, 0f, 0f, 0f, 0.4f, unlit = true)
    }
    val isMoto = v.type.equals("Motorcycle", true)
    val w = if (isMoto) 0.75f else 1.9f
    val h = if (isMoto) 0.75f else 1.05f
    val l = if (isMoto) 2.1f else 3.6f
    drawCube(v.x, h * 0.55f, v.z, w, h, l, -v.yawDeg, v.colorRgb[0], v.colorRgb[1], v.colorRgb[2], 1f)
    if (!isMoto) {
      drawCube(v.x, h + 0.4f, v.z, w * 0.85f, 0.7f, l * 0.55f, -v.yawDeg, 0.14f, 0.22f, 0.30f, 0.92f)
    }
    // Wheels
    drawCube(v.x, 0.32f, v.z, w + 0.2f, 0.55f, l * 0.75f, -v.yawDeg, 0.1f, 0.1f, 0.1f, 1f)
  }

  private fun drawCube(
    x: Float,
    y: Float,
    z: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    yawDeg: Float,
    r: Float,
    g: Float,
    b: Float,
    a: Float,
    unlit: Boolean = false,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    if (yawDeg != 0f) Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

    GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0)
    GLES20.glUniform4f(colorHandle, r, g, b, a)
    GLES20.glUniform1f(unlitHandle, if (unlit) 1f else 0f)

    bindAndDraw(meshes.cubeBuffer, meshes.cubeVertexCount)
  }

  private fun drawCylinderWall(
    x: Float,
    y: Float,
    z: Float,
    radius: Float,
    height: Float,
    r: Float,
    g: Float,
    b: Float,
    a: Float,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, radius, height, radius)
    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

    GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0)
    GLES20.glUniform4f(colorHandle, r, g, b, a)
    GLES20.glUniform1f(unlitHandle, 1f)

    bindAndDraw(meshes.cylinderWallBuffer, meshes.cylinderWallVertexCount)
  }

  private fun drawDisc(
    x: Float,
    y: Float,
    z: Float,
    radius: Float,
    r: Float,
    g: Float,
    b: Float,
    a: Float,
    unlit: Boolean = false,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, radius, 1f, radius)
    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

    GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0)
    GLES20.glUniform4f(colorHandle, r, g, b, a)
    GLES20.glUniform1f(unlitHandle, if (unlit) 1f else 0f)

    bindAndDraw(meshes.discBuffer, meshes.discVertexCount)
  }

  private fun bindAndDraw(buffer: FloatBuffer, vertexCount: Int) {
    val stride = 6 * 4
    buffer.position(0)
    GLES20.glEnableVertexAttribArray(posHandle)
    GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, stride, buffer)

    buffer.position(3)
    GLES20.glEnableVertexAttribArray(normHandle)
    GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, stride, buffer)

    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
  }

  private fun compileShader(type: Int, src: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, src)
    GLES20.glCompileShader(shader)
    return shader
  }
}

@Composable
fun LobbyCharacter3DView(
  character: CatalogCharacter,
  weapon: CatalogWeapon?,
  settings: LocalPlayerSettings,
  modifier: Modifier = Modifier,
) {
  val latestCharacter = androidx.compose.runtime.rememberUpdatedState(character)
  val latestWeapon = androidx.compose.runtime.rememberUpdatedState(weapon)
  val latestSettings = androidx.compose.runtime.rememberUpdatedState(settings)

  val renderer = remember {
    BattleRoyaleGLRenderer(
      isLobbyShowcase = true,
      simulationProvider = { null },
      showcaseCharacterProvider = { latestCharacter.value },
      showcaseWeaponProvider = { latestWeapon.value },
      settingsProvider = { latestSettings.value },
      onHudSnapshot = {},
    )
  }
  var glViewRef: GLSurfaceView? = remember { null }

  DisposableEffect(Unit) {
    onDispose {
      glViewRef?.onPause()
    }
  }

  AndroidView(
    modifier = modifier.pointerInput(Unit) {
      detectDragGestures { change, dragAmount ->
        change.consume()
        renderer.showcaseOrbitYawDeg = (renderer.showcaseOrbitYawDeg + dragAmount.x * 0.45f) % 360f
      }
    },
    factory = { ctx: Context ->
      GLSurfaceView(ctx).apply {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glViewRef = this
      }
    },
    update = { view ->
      view.onResume()
    },
  )
}

@Composable
fun Battle3DViewport(
  simulation: BattleSimulation,
  settings: LocalPlayerSettings,
  onHudSnapshot: (HudSnapshot) -> Unit,
  modifier: Modifier = Modifier,
) {
  var glViewRef: GLSurfaceView? = remember { null }

  DisposableEffect(Unit) {
    onDispose {
      glViewRef?.onPause()
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx: Context ->
      GLSurfaceView(ctx).apply {
        setEGLContextClientVersion(2)
        setRenderer(
          BattleRoyaleGLRenderer(
            isLobbyShowcase = false,
            simulationProvider = { simulation },
            showcaseCharacterProvider = { simulation.playerCharacter },
            showcaseWeaponProvider = { simulation.currentWeapon() },
            settingsProvider = { settings },
            onHudSnapshot = onHudSnapshot,
          )
        )
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glViewRef = this
      }
    },
    update = { view ->
      view.onResume()
    },
  )
}
