package org.mesdag.particlestorm.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;
import org.mesdag.particlestorm.data.molang.compiler.value.VariableAssignment;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.particlestorm.particle.ParticleVariableTable;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the Molang assignment pre-initialization mechanism
 * (b1e17f2 commented it out; dream1's emitter_shape_point.offset then reads
 * variable.s before particle_initialization assigns it).
 */
class MolangAssignmentPreInitRegressionTest {

    private static final float EPS = 1e-3F;

    private static final class FakeMolangInstance implements MolangInstance {
        private final VariableTable vars;
        private final float random1;

        private FakeMolangInstance(VariableTable vars, float random1) {
            this.vars = vars;
            this.random1 = random1;
        }

        @Override
        public VariableTable getVars() {
            return vars;
        }

        @Override
        public Level getLevel() {
            return null;
        }

        @Override
        public float tickAge() {
            return 0;
        }

        @Override
        public float tickLifetime() {
            return 0;
        }

        @Override
        public float getRandom1() {
            return random1;
        }

        @Override
        public float getRandom2() {
            return 0;
        }

        @Override
        public float getRandom3() {
            return 0;
        }

        @Override
        public float getRandom4() {
            return 0;
        }

        @Override
        public ResourceLocation getIdentity() {
            return null;
        }

        @Override
        public Vec3 getPosition() {
            return Vec3.ZERO;
        }

        @Override
        public Entity getAttachedEntity() {
            return null;
        }

        @Override
        public float getInvTickRate() {
            return 1.0F;
        }

        @Override
        public ParticleEmitter getEmitter() {
            return null;
        }
    }

    private static List<VariableAssignment> discoverAssignments(MolangExp exp, MolangParser parser, VariableTable table) {
        exp.compile(parser);
        MathValue root = exp.getVariable();
        List<VariableAssignment> toInit = new ArrayList<>();
        if (root != null && !MathHelper.forAssignment(table.table, toInit, root)) {
            MathHelper.forCompound(table.table, toInit, root);
        }
        return toInit;
    }

    /**
     * Locks ParticlePreset discovery: assignments inside a particle expression
     * (dream1's particle_initialization per_render_expression) must be collected
     * into preset.assignments and registered in the preset variable table.
     */
    @Test
    void particlePresetDiscoversAssignments() {
        VariableTable presetVars = new VariableTable(new Hashtable<>(), null);
        presetVars.setValue("variable.particle_random_1", instance -> 0.5F);
        MolangParser parser = new MolangParser(presetVars);
        MolangExp perRender = new MolangExp("variable.s = variable.particle_random_1 * 360; variable.k = 2");

        List<VariableAssignment> toInit = discoverAssignments(perRender, parser, presetVars);

        assertEquals(2, toInit.size(), "all assignments in the compound must be discovered");
        assertEquals("variable.s", toInit.get(0).name());
        assertEquals("variable.k", toInit.get(1).name());
        assertTrue(presetVars.table.containsKey("variable.s"), "assigned variable must be registered in the preset table");

        FakeMolangInstance instance = new FakeMolangInstance(presetVars, 0.5F);
        assertEquals(180.0F, presetVars.getValue("variable.s", instance), EPS,
                "preset table must evaluate the assignment RHS for the instance");
    }

    /**
     * Core dream1 regression: emitter_shape_point.offset evaluates
     * math.cos(variable.s) before particle_initialization runs. The spawn-time
     * redirect (EmitterShape.emittingParticle) must bind variable.s into the
     * particle instance variable table so the offset reads 180 degrees, not 0.
     */
    @Test
    void redirectBindsAssignmentBeforeShapeOffsetEvaluation() {
        VariableTable particlePresetVars = new VariableTable(new Hashtable<>(), null);
        particlePresetVars.setValue("variable.particle_random_1", instance -> 0.5F);
        MolangParser particleParser = new MolangParser(particlePresetVars);
        MolangExp perRender = new MolangExp("variable.s = variable.particle_random_1 * 360");
        List<VariableAssignment> toInit = discoverAssignments(perRender, particleParser, particlePresetVars);
        assertFalse(toInit.isEmpty(), "variable.s assignment must be discovered");

        VariableTable emitterPresetVars = new VariableTable(new Hashtable<>(), null);
        MolangExp offsetX = new MolangExp("math.cos(variable.s)");
        offsetX.compile(new MolangParser(emitterPresetVars));

        VariableTable emitterVars = new VariableTable(emitterPresetVars);
        ParticleVariableTable particleVars = new ParticleVariableTable(particlePresetVars, emitterVars);
        FakeMolangInstance instance = new FakeMolangInstance(particleVars, 0.5F);

        MathHelper.redirect(toInit, particleVars);

        assertEquals(180.0F, particleVars.getValue("variable.s", instance), EPS,
                "variable.s must be bound before offset evaluation");
        assertEquals(-1.0F, offsetX.calculate(instance), EPS,
                "offset math.cos(variable.s) must evaluate with the assigned 180 degrees");
        assertEquals(180.0F, particleVars.getValue("variable.s", instance), EPS,
                "re-reading the redirected RHS keeps variable.s at 180 for this instance");
    }

    /**
     * Locks the emitter-level path: assignments in emitter components
     * (EmitterPreset discovery) and in the emitter expression must be bound into
     * the emitter variable table by ParticleEmitter.initVars redirect.
     */
    @Test
    void emitterAssignmentsAreDiscoveredAndRedirected() {
        VariableTable emitterPresetVars = new VariableTable(new Hashtable<>(), null);
        MolangParser parser = new MolangParser(emitterPresetVars);
        MolangExp rateExp = new MolangExp("variable.emitter_test = 3");
        List<VariableAssignment> toInit = discoverAssignments(rateExp, parser, emitterPresetVars);

        assertEquals(1, toInit.size());
        assertEquals("variable.emitter_test", toInit.get(0).name());

        VariableTable emitterVars = new VariableTable(emitterPresetVars);
        MathHelper.redirect(toInit, emitterVars);
        FakeMolangInstance instance = new FakeMolangInstance(emitterVars, 0.0F);
        assertEquals(3.0F, emitterVars.getValue("variable.emitter_test", instance), EPS,
                "emitter-level assignment must be readable from the emitter variable table");
    }
}
