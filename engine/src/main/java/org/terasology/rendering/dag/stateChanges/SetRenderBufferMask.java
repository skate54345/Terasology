/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.dag.stateChanges;

import org.terasology.assets.ResourceUrn;
import org.terasology.rendering.opengl.BaseFBOsManager;
import org.terasology.rendering.opengl.FBO;
import org.terasology.rendering.opengl.FBOManagerSubscriber;
import com.google.common.base.Objects;
import org.terasology.rendering.dag.RenderPipelineTask;
import org.terasology.rendering.dag.StateChange;

/**
 * This StateChange generates the tasks that set and reset render masks.
 *
 * Every FBO can either allow or forbid OpenGL from writing to its Color buffer, DepthStencil buffer and Light buffer.
 * This StateChange modifies the FBO to mask or unmask these three buffers, independent of each other.
 * Note that this StateChange assumes the given FBO has the required attachments, and trying to unmask the
 * depth buffer or light buffer for a normal FBO with default configuration -might- lead to undefined behaviour.
 */
public final class SetRenderBufferMask implements FBOManagerSubscriber, StateChange {
    private ResourceUrn fboName;
    private BaseFBOsManager fboManager;

    private boolean renderToColorBuffer;
    private boolean renderToDepthBuffer;
    private boolean renderToLightBuffer;

    private SetRenderBufferMask defaultInstance;
    private SetRenderBufferMaskTask task;

    /**
     * Creates an instance of this class with the given parameters.
     *
     * @param fboName A ResourceUrn identifying the FBO whose render masks have to be modified.
     * @param fboManager The FBOManager responsible for managing the given FBO.
     * @param renderToColorBuffer A boolean indicating whether the Color buffer of the given FBO should be written to.
     * @param renderToDepthBuffer A boolean indicating whether the DepthStencil buffer of the given FBO should be written to.
     * @param renderToLightBuffer A boolean indicating whether the Light buffer of the given FBO should be written to.
     */
    public SetRenderBufferMask(ResourceUrn fboName, BaseFBOsManager fboManager, boolean renderToColorBuffer, boolean renderToDepthBuffer, boolean renderToLightBuffer) {
        this.fboName = fboName;
        this.fboManager = fboManager;

        this.renderToColorBuffer = renderToColorBuffer;
        this.renderToDepthBuffer = renderToDepthBuffer;
        this.renderToLightBuffer = renderToLightBuffer;
    }

    /**
     * Creates the default instance of this class for the given FBO, resetting all masks to true.
     *
     * @param fboName A ResourceUrn identifying the FBO whose render masks have to be modified.
     * @param fboManager The FBOManager responsible for managing the given FBO.
     */
    private SetRenderBufferMask(ResourceUrn fboName, BaseFBOsManager fboManager) {
        this.fboName = fboName;
        this.fboManager = fboManager;

        this.renderToColorBuffer = true;
        this.renderToDepthBuffer = true;
        this.renderToLightBuffer = true;

        defaultInstance = this;
    }

    public ResourceUrn getFboName() {
        return fboName;
    }

    @Override
    public StateChange getDefaultInstance() {
        if (defaultInstance == null)
            defaultInstance = new SetRenderBufferMask(fboName, fboManager);
        return defaultInstance;
    }

    @Override
    public RenderPipelineTask generateTask() {
        if (task == null) {
            task = new SetRenderBufferMaskTask(fboManager.get(fboName), renderToColorBuffer, renderToDepthBuffer, renderToLightBuffer);
            fboManager.subscribe(this);
        }
        return task;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fboName);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof SetRenderBufferMask)
                && fboName.equals(((SetRenderBufferMask) obj).getFboName())
                && renderToColorBuffer == ((SetRenderBufferMask) obj).renderToColorBuffer
                && renderToDepthBuffer == ((SetRenderBufferMask) obj).renderToDepthBuffer
                && renderToLightBuffer == ((SetRenderBufferMask) obj).renderToLightBuffer;
    }

    @Override
    public void update() {
        task.setFbo(fboManager.get(fboName));
    }

    @Override
    public String toString() {
        return String.format("%30s: %s, %b, %b, %b", this.getClass().getSimpleName(), fboName, renderToColorBuffer, renderToDepthBuffer, renderToLightBuffer);
    }

    private final class SetRenderBufferMaskTask implements RenderPipelineTask {
        private FBO fbo;
        private boolean renderToColorBuffer;
        private boolean renderToDepthBuffer;
        private boolean renderToLightBuffer;

        private SetRenderBufferMaskTask(FBO fbo, boolean renderToColorBuffer, boolean renderToDepthBuffer, boolean renderToLightBuffer) {
            this.fbo = fbo;

            this.renderToColorBuffer = renderToColorBuffer;
            this.renderToDepthBuffer = renderToDepthBuffer;
            this.renderToLightBuffer = renderToLightBuffer;
        }

        @Override
        public void execute() {
            fbo.setRenderBufferMask(renderToColorBuffer, renderToDepthBuffer, renderToLightBuffer);
        }

        private void setFbo(FBO fbo) {
            this.fbo = fbo;
        }

        @Override
        public String toString() {
            return String.format("%30s: %s, %b, %b, %b", this.getClass().getSimpleName(), fboName, renderToColorBuffer, renderToDepthBuffer, renderToLightBuffer);
        }
    }
}
