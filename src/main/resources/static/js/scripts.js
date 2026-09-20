document.addEventListener('DOMContentLoaded', () => {
    
    // Lógica para botones que muestran/ocultan formularios con animación
    const toggleButtons = document.querySelectorAll('.btn-toggle-form');
    
    toggleButtons.forEach(button => {
        button.addEventListener('click', (e) => {
            const targetId = button.getAttribute('data-target');
            const targetForm = document.getElementById(targetId);
            
            if (targetForm) {
               // Usamos una clase CSS para manejar la visibilidad y animación
               targetForm.classList.toggle('form-visible');
               
               // Si se muestra, ponemos foco en el primer input
               if (targetForm.classList.contains('form-visible')) {
                   const firstInput = targetForm.querySelector('input:not([type="hidden"])');
                   if(firstInput) firstInput.focus();
               }
            }
        });
    });

    // Auto-submit para los checkboxes
    const autoChecks = document.querySelectorAll('.auto-submit-check');
    autoChecks.forEach(check => {
        check.addEventListener('change', () => {
            check.closest('form').submit();
        });
    });
});
function confirmarBorrado(url) {
    const modal = document.getElementById('modalConfirmar');

    // El borrado se envía por POST (con el token CSRF que ya lleva el formulario del modal)
    document.getElementById('formBorrado').action = url;
    
    // Mostramos el modal
    modal.style.display = 'flex';
}

function cerrarModal() {
    document.getElementById('modalConfirmar').style.display = 'none';
}