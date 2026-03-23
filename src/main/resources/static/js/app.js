function showToast(message, type = 'success') {
    const container = document.querySelector('.toast-container') || createToastContainer();
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    window.setTimeout(() => {
        toast.classList.add('toast-hide');
        window.setTimeout(() => toast.remove(), 240);
    }, 2600);
}

function createToastContainer() {
    const container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
    return container;
}

function updatePreviewImage(targetInput) {
    if (!targetInput) {
        return;
    }
    const wrapper = targetInput.closest('.editor-block, .meal-editor');
    if (!wrapper) {
        return;
    }
    let preview = wrapper.querySelector('.preview-image');
    const value = targetInput.value ? targetInput.value.trim() : '';
    if (!value) {
        if (preview) {
            preview.remove();
        }
        return;
    }
    if (!preview) {
        preview = document.createElement('img');
        preview.className = 'preview-image';
        preview.alt = 'preview';
        targetInput.parentElement.parentElement.insertAdjacentElement('afterend', preview);
    }
    preview.src = value;
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.file-upload').forEach(input => {
        input.addEventListener('change', async event => {
            const file = event.target.files[0];
            if (!file) {
                return;
            }
            const formData = new FormData();
            formData.append('file', file);
            try {
                const response = await fetch('/api/admin/upload', {
                    method: 'POST',
                    body: formData
                });
                if (!response.ok) {
                    throw new Error('上传接口调用失败');
                }
                const result = await response.json();
                const targetName = event.target.dataset.target;
                const targetInput = document.querySelector(`[name='${targetName}']`);
                if (targetInput) {
                    targetInput.value = result.path || '';
                    updatePreviewImage(targetInput);
                }
                showToast(result.path ? '图片上传成功。' : '上传完成，但未返回图片地址。', result.path ? 'success' : 'error');
            } catch (error) {
                showToast(error.message || '图片上传失败，请稍后重试。', 'error');
            }
        });
    });

    document.querySelectorAll('.ai-generate').forEach(button => {
        button.addEventListener('click', async event => {
            const promptName = event.target.dataset.promptTarget;
            const resultName = event.target.dataset.resultTarget;
            const promptInput = document.querySelector(`[name='${promptName}']`);
            const resultInput = document.querySelector(`[name='${resultName}']`);
            if (!promptInput || !promptInput.value.trim()) {
                showToast('请先填写 AI 配图提示词。', 'error');
                return;
            }
            const formData = new FormData();
            formData.append('prompt', promptInput.value.trim());
            try {
                const response = await fetch('/api/admin/ai-image', {
                    method: 'POST',
                    body: formData
                });
                if (!response.ok) {
                    throw new Error('AI 配图请求失败');
                }
                const result = await response.json();
                if (resultInput) {
                    resultInput.value = result.path || '';
                    updatePreviewImage(resultInput);
                }
                showToast(result.path ? 'AI 配图完成。' : 'AI 配图未返回图片地址。', result.path ? 'success' : 'error');
            } catch (error) {
                showToast(error.message || 'AI 配图失败，请稍后重试。', 'error');
            }
        });
    });
});
