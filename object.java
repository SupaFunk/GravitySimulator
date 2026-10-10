import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;


public class object {
    public final double G = 6.67430e-11;
    private float x, y, z;
    private float xV, yV, zV;
    private final float mass;
    private final float radius;
    private final int res;


    private float[] vertices;
    private int[] indices;
    private int vao, vbo, ebo;
    private int shaderProgram;
    private int modelLocation;
    private int viewLocation;
    private int projectionLocation;

    public object(float x, float y, float z, float r, int res, float vx, float vy, float vz, float m) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = r;
        this.res = res;
        this.xV = vx;
        this.yV = vy;
        this.zV = vz;
        this.mass = m;

        buildMesh();
        initGL();
        initShaders();

    }
    
    private void buildMesh() {
        vertices = new float[res * 9];
        indices = new int[res * 3];
        int latRes = res;
        int lonRes = res;

        int vertexCount = (latRes + 1) * (lonRes + 1);
        vertices = new float[vertexCount * 3];

        int indexCount = latRes * lonRes * 6;
        indices = new int[indexCount];

        int v = 0;
        for (int i = 0; i <= latRes; i++) {
            float theta = (float) (Math.PI * i / latRes);

            for (int j = 0; j <= lonRes; j++) {
                float phi = (float) (2 * Math.PI * j / lonRes);

                float px = (float)(Math.sin(theta) * Math.cos(phi));
                float py = (float)Math.cos(theta);
                float pz = (float)(Math.sin(theta) * Math.sin(phi));

                vertices[v++] = px;
                vertices[v++] = py;
                vertices[v++] = pz;
            }
        }

        int k = 0;
        for (int i = 0; i < latRes; i++) {
            for (int j = 0; j < lonRes; j++) {
                int first = i * (lonRes + 1) + j;
                int second = first + lonRes + 1;

                indices[k++] = first;
                indices[k++] = second;
                indices[k++] = first + 1;

                indices[k++] = second;
                indices[k++] = second + 1;
                indices[k++] = first + 1;
            }
        }
    }

    private void initGL() {
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
    }

    private void initShaders() {
        String vs =
                "#version 330 core\n" +
                        "layout(location=0) in vec3 aPos;\n" +
                        "\n" +
                        "uniform mat4 model;\n" +
                        "uniform mat4 view;\n" +
                        "uniform mat4 projection;\n" +
                        "\n" +
                        "out vec3 FragPos;\n" +
                        "out vec3 Normal;\n" +
                        "\n" +
                        "void main(){\n" +
                        "    FragPos = vec3(model * vec4(aPos, 1.0));\n" +
                        "    Normal = aPos; // unit sphere = valid normal\n" +
                        "    gl_Position = projection * view * vec4(FragPos, 1.0);\n" +
                        "}";

        String fs =
                "#version 330 core\n" +
                        "in vec3 FragPos;\n" +
                        "in vec3 Normal;\n" +
                        "out vec4 FragColor;\n" +
                        "\n" +
                        "void main(){\n" +
                        "    vec3 lightDir = normalize(vec3(1.0, 1.0, 1.0));\n" +
                        "    float diff = max(dot(normalize(Normal), lightDir), 0.2);\n" +
                        "    vec3 baseColor = vec3(0.2, 0.6, 1.0);\n" +
                        "    FragColor = vec4(baseColor * diff, 1.0);\n" +
                        "}";

        int v = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(v, vs);
        glCompileShader(v);

        int f = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(f, fs);
        glCompileShader(f);

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, v);
        glAttachShader(shaderProgram, f);
        glLinkProgram(shaderProgram);

        modelLocation = glGetUniformLocation(shaderProgram, "model");
        viewLocation = glGetUniformLocation(shaderProgram, "view");
        projectionLocation = glGetUniformLocation(shaderProgram, "projection");
    }

    public void render(Matrix4f view, Matrix4f projection) {
        glUseProgram(shaderProgram);
        glBindVertexArray(vao);

        float renderScale = 1.0e-7f;

        Matrix4f model = new Matrix4f()
                .translate((x * renderScale),
                        (y * renderScale),
                        (z * renderScale))
                .scale(radius * renderScale * 5.0f);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(modelLocation,      false, model.get(stack.mallocFloat(16)));
            glUniformMatrix4fv(viewLocation,       false, view.get(stack.mallocFloat(16)));
            glUniformMatrix4fv(projectionLocation, false, projection.get(stack.mallocFloat(16)));
        }

        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
    }
    public void update(float dt) {
        x += xV * dt;
        y += yV * dt;
        z += zV * dt;
    }
    public static void applyGravity(ArrayList<object> k, float dt) {

        // simulation gravity scale (NOT real-world)
        float G = 1f;

        for (int i = 0; i < k.size(); i++) {
            object a = k.get(i);

            for (int j = i + 1; j < k.size(); j++) {
                object b = k.get(j);

                float dx = b.x - a.x;
                float dy = b.y - a.y;
                float dz = b.z - a.z;

                // stable distance (prevents underflow + divide issues)
                float distSq = dx*dx + dy*dy + dz*dz + 0.01f;
                float dist = (float)Math.sqrt(distSq);

                float nx = dx / dist;
                float ny = dy / dist;
                float nz = dz / dist;

                // shared gravity factor (prevents mismatch + drift)
                float invDistSq = 1.0f / distSq;
                float g = G * invDistSq;

                float accelA = g * b.mass;
                float accelB = g * a.mass;

                // apply acceleration -> velocity
                a.xV += nx * accelA * dt;
                a.yV += ny * accelA * dt;
                a.zV += nz * accelA * dt;

                b.xV -= nx * accelB * dt;
                b.yV -= ny * accelB * dt;
                b.zV -= nz * accelB * dt;
            }
        }

        for (object o : k) {
            System.out.println(o.z);
            System.out.println(o.zV * dt);
            o.x += o.xV * dt;
            o.y += o.yV * dt;
            o.z += o.zV * dt;

        }
    }
    //only works for 2d right now may be fixed
    public static void collision(object a, object b) {

        float dx = b.x - a.x;
        float dy = b.y - a.y;


        float minDist = a.radius + b.radius;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        if (dist>= minDist) return;


        if (dist == 0) {
            dx = 1;
            dy = 0;
            dist = 1;
        }

        float nx = dx / dist;
        float ny = dy / dist;

        float rvx = b.xV - a.xV;
        float rvy = b.yV - a.yV;

        float velAlongNormal = rvx * nx + rvy * ny;

        if (velAlongNormal > 0) return;

        float e = 1.0f;

        float j = -(1 + e) * velAlongNormal;
        j /= (1 / a.mass + 1 / b.mass);

        float ix = j * nx;
        float iy = j * ny;

        a.xV -= ix / a.mass;
        a.yV -= iy / a.mass;

        b.xV += ix / b.mass;
        b.yV += iy / b.mass;

        float overlap = minDist - dist;

        a.x -= nx * overlap * 0.5f;
        a.y -= ny * overlap * 0.5f;

        b.x += nx * overlap * 0.5f;
        b.y += ny * overlap * 0.5f;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getMass() { return mass; };
    public float getXV() { return xV; }
    public float getYV() { return yV; }
    public float getRadius() { return radius; }

    public void setXV(float v) { xV = v; }
    public void setYV(float v) { yV = v; }
}