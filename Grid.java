import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.ArrayList;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public class Grid {

    private static final double G = 6.67430e-11;
    private static final double C = 299792458.0;

    private int rSteps, zSteps;
    private float maxSize;
    private int shader;

    private float[] vertices;
    private int vao, vbo;
    private float spacing = 5e10f;// visual bending strength


    private final float[] modelBuf = new float[16];
    private final float[] viewBuf = new float[16];
    private final float[] projBuf = new float[16];

    public Grid(int rSteps, int zSteps, int shader) {
        this.rSteps = rSteps;
        this.zSteps = zSteps;
        this.shader = shader;

        this.maxSize = 5e6f;

        initGL();
    }

    // Schwarzschild radius
    private float rs(float mass) {
        return (float)((2.0 * G * mass) / (C * C));
    }

    public void update(ArrayList<object> objects) {
        build(objects);
    }

    private void build(ArrayList<object> objects) {

        vertices = new float[rSteps * zSteps * 3];
        int idx = 0;

        float halfX = rSteps * 0.5f;
        float halfZ = zSteps * 0.5f;

        for (int i = 0; i < rSteps; i++) {

            float x = (i - halfX) * spacing;

            for (int j = 0; j < zSteps; j++) {

                float z = (j - halfZ) * spacing;

                float sink = 0.0f;


                float y = 0.0f;

                for (object obj : objects) {

                    /*double dx = x - obj.getX();
                    double dz = z - obj.getZ();

                    double r = Math.sqrt(dx * dx + dz * dz) + 1e3; //softening

                    double rs = rs(obj.getMass());

                    double delta = Math.max(r - rs, 1e-3);

                    //flamm's paraboloid
                    double contribution = 2.0 * Math.sqrt(rs * delta); */

                    //visual scaling
                    sink += (float) computeSink(obj,x,z);
                    //y += (float)(contribution * 1e-3);
                }
                //System.out.println(sink*1e10f);
                y += sink * 2e5f; //change scale based on center mass for more significant curve
                y += 4e5f; //change to get desired location
                //store vertices
                vertices[idx++] = x * 1e-7f;
                vertices[idx++] = y;
                vertices[idx++] = z * 1e-7f;
            }
        }

        upload();
    }

    private double computeSink(object obj, float x, float z) {

        double rs = rs(obj.getMass());
        rs = Math.max(rs, 1e-6);

        double dx = x - obj.getX();
        double dz = z - obj.getZ();
        double r = Math.sqrt(dx * dx + dz * dz) / spacing;
        r += 1e-3;// match vertex scale
        
        double base = -Math.sqrt(rs) / (1.0 + r);;

        return base;
    }

    private void upload() {

        if (vao == 0) {
            vao = glGenVertexArrays();
            vbo = glGenBuffers();
        }

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);
    }


    public void render(Matrix4f view, Matrix4f projection) {

        glUseProgram(shader);
        glBindVertexArray(vao);

        int modelLoc = glGetUniformLocation(shader, "model");
        int viewLoc = glGetUniformLocation(shader, "view");
        int projLoc = glGetUniformLocation(shader, "projection");

        Matrix4f model = new Matrix4f();

        model.get(modelBuf);
        view.get(viewBuf);
        projection.get(projBuf);

        glUniformMatrix4fv(modelLoc, false, modelBuf);
        glUniformMatrix4fv(viewLoc, false, viewBuf);
        glUniformMatrix4fv(projLoc, false, projBuf);

        glPointSize(2.0f);
        glDrawArrays(GL_POINTS, 0, vertices.length / 3);
    }

    private void initGL() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
    }
}