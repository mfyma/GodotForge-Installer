@tool
extends EditorPlugin

var dock: VBoxContainer

func _enter_tree() -> void:
    dock = VBoxContainer.new()
    dock.name = "GodotForge AI"

    var title := Label.new()
    title.text = "GodotForge AI"
    title.add_theme_font_size_override("font_size", 20)
    dock.add_child(title)

    var status := Label.new()
    status.text = "The addon is installed and ready. No API keys are bundled."
    status.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    dock.add_child(status)

    add_control_to_dock(DOCK_SLOT_RIGHT_UL, dock)

func _exit_tree() -> void:
    if is_instance_valid(dock):
        remove_control_from_docks(dock)
        dock.queue_free()
