import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import java.util.*;
import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.opengl.GL15.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class HelloWorld {

    private long window;
    float screenWidth  = 800;
    float screenHeight = 600;

    private Camera camera;

    public void run() {
        init();
        main();
    }

    private void init() {
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow((int)screenWidth, (int)screenHeight, "grav sim", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);


        camera = new Camera(0.0f, 5.0f, 20.0f);
        camera.captureMouse(window);

        // Mouse movement callback
        glfwSetCursorPosCallback(window, (win, xpos, ypos) ->
                camera.processMouseMovement(xpos, ypos));

        // Scroll callback for zoom
        glfwSetScrollCallback(window, (win, xOffset, yOffset) ->
                camera.processScroll(yOffset));

        glfwSetFramebufferSizeCallback(window, (win, width, height) ->
                glViewport(0, 0, width, height));

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
    }

    private int main() {
        object circle = new object(
                0f, 0f, 0f,
                6.957e9f,       // radius
                100,
                0f, 0f, 0f,
                1.989e30f
        );

        object circle1 = new object(
                0f, 0f, 1.5e12f,   // x y z
                6e9f,            // radius
                100,
                297800000f, 0f, 0f,      // orbital velocity
                6.67e25f            // mass
        );
        object third = new object(
                0f, 0f, 2e12f,
                6.371e9f,   // radius in meters
                100,
                2978000000f, 0f,0f,
                5.972e30f
        );
        ArrayList<object> gr = new ArrayList<>();
        gr.add(circle);
        //gr.add(circle1);
        gr.add(third);
        double last = glfwGetTime();
        int gridShader = createGridShader();
        Grid grid = new Grid(300, 300, gridShader);
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float timeScale = 1000000f;
            float dt = 1f;
            last = now;
            object.applyGravity(gr,dt);

            //object.collision(circle, circle1);
                if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
                glfwSetWindowShouldClose(window, true);
            camera.processKeyboard(window);
            //delta time

            // Camera
            Matrix4f view = camera.getViewMatrix();
            Matrix4f projection = camera.getProjectionMatrix(screenWidth / screenHeight);

            // Render
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            for (object o : gr) {
                o.render(view, projection);
            }
            grid.update(gr);
            grid.render(view, projection);
            glfwSwapBuffers(window);
            glfwPollEvents();

        }

        glfwDestroyWindow(window);
        glfwTerminate();
        return 0;
    }
    private int createGridShader() {

        String vs =
                "#version 330 core\n" +
                        "layout(location=0) in vec3 aPos;\n" +
                        "uniform mat4 model;\n" +
                        "uniform mat4 view;\n" +
                        "uniform mat4 projection;\n" +
                        "void main(){ gl_Position = projection * view * model * vec4(aPos,1.0); }";

        String fs =
                "#version 330 core\n" +
                        "out vec4 FragColor;\n" +
                        "void main(){ FragColor = vec4(1.0); }";

        int v = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(v, vs);
        glCompileShader(v);

        if (glGetShaderi(v, GL_COMPILE_STATUS) == GL_FALSE) {
            System.out.println("VERTEX SHADER ERROR:");
            System.out.println(glGetShaderInfoLog(v));
        }

        int f = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(f, fs);
        glCompileShader(f);

        if (glGetShaderi(f, GL_COMPILE_STATUS) == GL_FALSE) {
            System.out.println("FRAGMENT SHADER ERROR:");
            System.out.println(glGetShaderInfoLog(f));
        }

        int program = glCreateProgram();
        glAttachShader(program, v);
        glAttachShader(program, f);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            System.out.println("SHADER LINK ERROR:");
            System.out.println(glGetProgramInfoLog(program));
        }

        glDeleteShader(v);
        glDeleteShader(f);

        return program;
    }
    public static void main(String[] args) {
        new HelloWorld().run();
    }
}