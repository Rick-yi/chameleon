/* chameleon 高保真原型 · 轻量交互脚本 */
function openModal(id) { var el = document.getElementById(id); if (el) el.classList.add('show'); }
function closeModal(id) { var el = document.getElementById(id); if (el) el.classList.remove('show'); }
document.addEventListener('click', function (e) {
  // 点击模态遮罩关闭
  if (e.target.classList && e.target.classList.contains('modal-overlay')) {
    e.target.classList.remove('show');
  }
  // 点名状态按钮组
  var asBtn = e.target.closest('.as-btn');
  if (asBtn) {
    var group = asBtn.closest('.attend-status');
    group.querySelectorAll('.as-btn').forEach(function (b) { b.className = 'as-btn'; });
    asBtn.classList.add(asBtn.dataset.active || 'active-green');
  }
});
