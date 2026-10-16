// Writes LoadShift/Localizable.xcstrings in exactly the form Xcode uses, so that Xcode does not
// rewrite it after every change made with other tools (which showed up as a changed file in git).
//
// Xcode's form: string keys ordered with `localizedStandardCompare`, all other keys alphabetically,
// two spaces indentation, " : " between key and value, an empty object as "{" + empty line + "}",
// no line break at the end of the file.
//
// Usage (from the repository root): swift tools/strings/normalize.swift [file]
// `./build.sh` runs it before building and committing.
import Foundation

let path = CommandLine.arguments.dropFirst().first ?? "LoadShift/Localizable.xcstrings"
let url = URL(fileURLWithPath: path)
let original = try String(contentsOf: url, encoding: .utf8)
let root = try JSONSerialization.jsonObject(with: Data(original.utf8))

func quoted(_ s: String) -> String {
    var out = "\""
    for scalar in s.unicodeScalars {
        switch scalar {
        case "\"": out += "\\\""
        case "\\": out += "\\\\"
        case "\n": out += "\\n"
        case "\r": out += "\\r"
        case "\t": out += "\\t"
        case let c where c.value < 0x20: out += String(format: "\\u%04x", c.value)
        default: out.unicodeScalars.append(scalar)
        }
    }
    return out + "\""
}

func write(_ value: Any, indent: Int, parentKey: String?) -> String {
    let pad = String(repeating: "  ", count: indent)
    switch value {
    case let dict as [String: Any]:
        if dict.isEmpty { return "{\n\n\(pad)}" }
        let keys = parentKey == "strings" && indent == 1
            ? dict.keys.sorted { $0.localizedStandardCompare($1) == .orderedAscending }
            : dict.keys.sorted()
        let items = keys.map { "\(pad)  \(quoted($0)) : \(write(dict[$0]!, indent: indent + 1, parentKey: $0))" }
        return "{\n" + items.joined(separator: ",\n") + "\n\(pad)}"
    case let array as [Any]:
        if array.isEmpty { return "[\n\n\(pad)]" }
        return "[\n" + array.map { "\(pad)  " + write($0, indent: indent + 1, parentKey: nil) }.joined(separator: ",\n") + "\n\(pad)]"
    case let string as String:
        return quoted(string)
    case let number as NSNumber:
        if CFGetTypeID(number) == CFBooleanGetTypeID() { return number.boolValue ? "true" : "false" }
        return number.stringValue
    default:
        return "null"
    }
}

let result = write(root, indent: 0, parentKey: nil)
if result != original {
    try result.write(to: url, atomically: true, encoding: .utf8)
    print("normalized \(path)")
}
