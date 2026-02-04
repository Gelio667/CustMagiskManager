use anyhow::Result;
use rand::RngCore;
use serde::Serialize;
use sha2::{Digest, Sha256};
use std::env;
use std::fs::{self, File, OpenOptions};
use std::io::{self, Read, Write};
use std::path::Path;
use std::process::Command;
use std::thread::sleep;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

fn ts() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis() as u64
}

fn log(level: &str, msg: &str) {
    let mut out = io::stdout().lock();
    let _ = writeln!(out, "{}|{}|{}", ts(), level, msg);
    let _ = out.flush();
}

fn data<T: Serialize>(v: &T) {
    if let Ok(s) = serde_json::to_string(v) {
        let mut out = io::stdout().lock();
        let _ = writeln!(out, "{}|DATA|{}", ts(), s);
        let _ = out.flush();
    }
}

fn sh_capture(cmd: &str) -> Option<String> {
    let out = Command::new("sh").arg("-c").arg(cmd).output().ok()?;
    if !out.status.success() {
        return None;
    }
    let s = String::from_utf8_lossy(&out.stdout).trim().to_string();
    if s.is_empty() { None } else { Some(s) }
}

fn cmd_capture(bin: &str, args: &[&str]) -> Option<String> {
    let out = Command::new(bin).args(args).output().ok()?;
    if !out.status.success() {
        return None;
    }
    let s = String::from_utf8_lossy(&out.stdout).trim().to_string();
    if s.is_empty() { None } else { Some(s) }
}

#[derive(Serialize)]
struct EnvInfo {
    uid: i32,
    su_path: Option<String>,
    su_v: Option<String>,
    su_cap_v: Option<String>,
    is_magisk_su: bool,
    magisk_path: Option<String>,
}

fn env_info() -> Result<i32> {
    let uid = unsafe { libc::getuid() as i32 };
    let su_path = sh_capture("command -v su");
    let su_v = cmd_capture("su", &["-v"]);
    let su_cap_v = cmd_capture("su", &["-V"]);
    let is_magisk_su = su_v.as_deref().unwrap_or("").to_ascii_uppercase().contains("MAGISKSU");
    let magisk_path = sh_capture("command -v magisk");
    data(&EnvInfo { uid, su_path, su_v, su_cap_v, is_magisk_su, magisk_path });
    log("OK", "env_ready");
    Ok(0)
}

fn sha256_cmd(path: &str) -> Result<i32> {
    log("INFO", "hash_open");
    let mut f = File::open(path)?;
    let mut buf = [0u8; 65536];
    let mut h = Sha256::new();
    loop {
        let n = f.read(&mut buf)?;
        if n == 0 { break; }
        h.update(&buf[..n]);
    }
    let hexsum = hex::encode(h.finalize());
    data(&hexsum);
    log("OK", "hash_ok");
    Ok(0)
}

#[derive(Serialize)]
struct ModuleState {
    id: String,
    exists: bool,
    disabled: bool,
    removed: bool,
}

fn module_state_cmd(id: &str) -> Result<i32> {
    let base = format!("/data/adb/modules/{}", id);
    let exists = Path::new(&base).exists();
    let disabled = Path::new(&format!("{}/disable", base)).exists();
    let removed = Path::new(&format!("{}/remove", base)).exists();
    data(&ModuleState { id: id.to_string(), exists, disabled, removed });
    if exists && !removed {
        log("OK", "module_state_ok");
        Ok(0)
    } else {
        log("WARN", "module_missing");
        Ok(1)
    }
}

fn module_disable_cmd(id: &str) -> Result<i32> {
    let base = format!("/data/adb/modules/{}", id);
    if !Path::new(&base).exists() {
        log("ERROR", "module_not_found");
        return Ok(1);
    }
    let p = format!("{}/disable", base);
    let _ = OpenOptions::new().create(true).write(true).open(&p)?;
    log("OK", "module_disabled");
    Ok(0)
}

fn module_enable_cmd(id: &str) -> Result<i32> {
    let base = format!("/data/adb/modules/{}", id);
    if !Path::new(&base).exists() {
        log("ERROR", "module_not_found");
        return Ok(1);
    }
    let p = format!("{}/disable", base);
    let _ = fs::remove_file(&p);
    log("OK", "module_enabled");
    Ok(0)
}

fn magisk_install_module(zip_path: &str) -> Result<i32> {
    log("INFO", "magisk_install_start");
    let out = Command::new("magisk").args(["--install-module", zip_path]).output();
    match out {
        Ok(o) => {
            let s = String::from_utf8_lossy(&o.stdout).trim().to_string();
            let e = String::from_utf8_lossy(&o.stderr).trim().to_string();
            if !s.is_empty() { log("INFO", &s); }
            if !e.is_empty() { log("WARN", &e); }
            if o.status.success() {
                log("OK", "magisk_install_ok");
                Ok(0)
            } else {
                log("ERROR", "magisk_install_failed");
                Ok(1)
            }
        }
        Err(_) => {
            log("ERROR", "magisk_binary_not_found");
            Ok(1)
        }
    }
}

fn magisk_remove_module(id: &str) -> Result<i32> {
    log("INFO", "magisk_remove_start");
    let out = Command::new("magisk").args(["--remove-module", id]).output();
    match out {
        Ok(o) => {
            let s = String::from_utf8_lossy(&o.stdout).trim().to_string();
            let e = String::from_utf8_lossy(&o.stderr).trim().to_string();
            if !s.is_empty() { log("INFO", &s); }
            if !e.is_empty() { log("WARN", &e); }
            if o.status.success() {
                log("OK", "magisk_remove_ok");
                Ok(0)
            } else {
                log("ERROR", "magisk_remove_failed");
                Ok(1)
            }
        }
        Err(_) => {
            log("ERROR", "magisk_binary_not_found");
            Ok(1)
        }
    }
}

fn magisk_remove_all_modules() -> Result<i32> {
    log("INFO", "magisk_remove_all_start");
    let out = Command::new("magisk").args(["--remove-modules"]).output();
    match out {
        Ok(o) => {
            let s = String::from_utf8_lossy(&o.stdout).trim().to_string();
            let e = String::from_utf8_lossy(&o.stderr).trim().to_string();
            if !s.is_empty() { log("INFO", &s); }
            if !e.is_empty() { log("WARN", &e); }
            if o.status.success() {
                log("OK", "magisk_remove_all_ok");
                Ok(0)
            } else {
                log("ERROR", "magisk_remove_all_failed");
                Ok(1)
            }
        }
        Err(_) => {
            log("ERROR", "magisk_binary_not_found");
            Ok(1)
        }
    }
}

fn module_api_cmd(action: &str, arg: &str, timeout_ms: u64) -> Result<i32> {
    let base = "/data/adb/CustMagisk";
    let api = format!("{}/api", base);
    let req = format!("{}/request.json", api);
    let res = format!("{}/response.json", api);

    if !Path::new(base).exists() {
        log("ERROR", "custmagisk_base_missing");
        return Ok(2);
    }
    if !Path::new(&api).exists() {
        log("ERROR", "custmagisk_api_missing");
        return Ok(2);
    }

    let mut rid = [0u8; 8];
    rand::thread_rng().fill_bytes(&mut rid);
    let rid_hex = hex::encode(rid);

    let payload = format!(
        "{{\"rid\":\"{}\",\"action\":\"{}\",\"arg\":\"{}\"}}",
        rid_hex,
        escape_json(action),
        escape_json(arg)
    );

    let _ = fs::remove_file(&res);
    fs::write(&req, payload.as_bytes())?;
    let _ = Command::new("sh").arg("-c").arg(format!("chmod 600 {}", req)).status();

    log("INFO", "module_api_sent");

    let start = ts();
    loop {
        if Path::new(&res).exists() {
            let txt = fs::read_to_string(&res).unwrap_or_default();
            if txt.contains(&format!("\"rid\":\"{}\"", rid_hex)) {
                data(&txt);
                log("OK", "module_api_ok");
                return Ok(0);
            }
        }
        if ts().saturating_sub(start) >= timeout_ms {
            log("ERROR", "module_api_timeout");
            return Ok(1);
        }
        sleep(Duration::from_millis(200));
    }
}

fn escape_json(s: &str) -> String {
    s.replace('\\', "\\\\").replace('"', "\\\"").replace('\n', "\\n").replace('\r', "\\r").replace('\t', "\\t")
}

fn usage() -> i32 {
    log("INFO", "commands: env | sha256 <path> | module_state <id> | module_enable <id> | module_disable <id> | module_install <zip> | module_remove <id> | modules_remove_all | module_api <action> <arg> [timeout_ms]");
    2
}

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() < 2 {
        std::process::exit(usage());
    }

    let res: Result<i32> = match args[1].as_str() {
        "env" => env_info(),
        "sha256" => {
            if args.len() < 3 { log("ERROR", "sha256_requires_path"); Ok(2) } else { sha256_cmd(&args[2]) }
        }
        "module_state" => {
            if args.len() < 3 { log("ERROR", "module_state_requires_id"); Ok(2) } else { module_state_cmd(&args[2]) }
        }
        "module_enable" => {
            if args.len() < 3 { log("ERROR", "module_enable_requires_id"); Ok(2) } else { module_enable_cmd(&args[2]) }
        }
        "module_disable" => {
            if args.len() < 3 { log("ERROR", "module_disable_requires_id"); Ok(2) } else { module_disable_cmd(&args[2]) }
        }
        "module_install" => {
            if args.len() < 3 { log("ERROR", "module_install_requires_zip"); Ok(2) } else { magisk_install_module(&args[2]) }
        }
        "module_remove" => {
            if args.len() < 3 { log("ERROR", "module_remove_requires_id"); Ok(2) } else { magisk_remove_module(&args[2]) }
        }
        "modules_remove_all" => magisk_remove_all_modules(),
        "module_api" => {
            if args.len() < 4 {
                log("ERROR", "module_api_requires_action_arg");
                Ok(2)
            } else {
                let to = if args.len() >= 5 { args[4].parse::<u64>().unwrap_or(8000) } else { 8000 };
                module_api_cmd(&args[2], &args[3], to)
            }
        }
        _ => Ok(usage()),
    };

    match res {
        Ok(code) => std::process::exit(code),
        Err(e) => {
            log("ERROR", &format!("panic:{}", e));
            std::process::exit(1);
        }
    }
}