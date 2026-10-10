import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class Camera {

    private Vector3f position;
    private Vector3f front;
    private Vector3f up;

    private float yaw   = -90.0f;
    private float pitch =   0.0f;
    private float speed       = 1000f;
    private float sensitivity = 0.1f;
    private float fov         = 45.0f;

    private double lastX = 400, lastY = 300;
    private boolean firstMouse = true;

    public Camera(float x, float y, float z) {
        position = new Vector3f(x, y, z);
        front    = new Vector3f(0.0f, 0.0f, -1.0f);
        up       = new Vector3f(0.0f, 1.0f,  0.0f);
    }

    // Call once in init() to hide and capture the cursor
    public void captureMouse(long window) {
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    // Call every frame with the GLFW window handle
    public void processKeyboard(long window) {
        // Normalize front projected onto XZ plane so vertical look doesn't affect speed
        Vector3f flatFront = new Vector3f(front.x, 0, front.z).normalize();
        Vector3f right     = new Vector3f(front).cross(up).normalize();

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
            position.add(new Vector3f(flatFront).mul(speed));
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
            position.sub(new Vector3f(flatFront).mul(speed));
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
            position.sub(new Vector3f(right).mul(speed));
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
            position.add(new Vector3f(right).mul(speed));

        // Up/down
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS)
            position.y += speed;
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS)
            position.y -= speed;
    }

    // Register this as a GLFW cursor callback
    public void processMouseMovement(double xpos, double ypos) {
        if (firstMouse) {
            lastX = xpos;
            lastY = ypos;
            firstMouse = false;
        }

        double xOffset = (xpos - lastX) * sensitivity;
        double yOffset = (lastY - ypos) * sensitivity; // reversed: y goes bottom to top
        lastX = xpos;
        lastY = ypos;

        yaw   += xOffset;
        pitch += yOffset;

        // Clamp pitch so screen doesn't flip
        if (pitch >  89.0f) pitch =  89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        // Recalculate front vector from yaw/pitch
        front.x = (float)(Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.y = (float)(Math.sin(Math.toRadians(pitch)));
        front.z = (float)(Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.normalize();
    }

    // Scroll to zoom
    public void processScroll(double yOffset) {
        fov -= (float) yOffset;
        if (fov < 1.0f)  fov = 1.0f;
        if (fov > 90.0f) fov = 90.0f;
    }

    public Matrix4f getViewMatrix() {
        // lookAt(eye, center, up)
        return new Matrix4f().lookAt(
                position,
                new Vector3f(position).add(front),
                up
        );
    }

    public Matrix4f getProjectionMatrix(float aspectRatio) {
        return new Matrix4f().perspective(
                (float) Math.toRadians(fov),
                aspectRatio,
                0.1f,
                1e9f
        );
    }
}