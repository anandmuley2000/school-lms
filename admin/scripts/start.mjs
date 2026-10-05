import { spawn } from "node:child_process";

const port = process.env.PORT;
if (!port) {
  throw new Error("Railway must provide PORT before starting the admin frontend.");
}

const server = spawn(
  process.execPath,
  ["./node_modules/serve/build/main.js", "-s", "dist", "-l", port],
  { stdio: "inherit" },
);

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => server.kill(signal));
}

server.on("error", (error) => {
  console.error("Could not start the local serve dependency:", error);
  process.exitCode = 1;
});
server.on("exit", (code, signal) => {
  process.exitCode = code ?? (signal ? 1 : 0);
});
