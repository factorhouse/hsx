module.exports = {
    testEnvironment: "jsdom", // Simulates a browser-like environment
    moduleDirectories: ["node_modules"], // Resolve modules like React and testing libraries
    transform: {
        "^.+\\.jsx?$": "babel-jest", // Use babel-jest for .js and .jsx files
    },
    silent: false, // Show console.log outputs
    presets: [
        ["@babel/preset-env", { targets: { node: "current" } }], // For modern JS
        "@babel/preset-react" // For JSX
    ],
    testMatch: ["**/*.test.js"], // Match test files ending with .test.js
    moduleFileExtensions: ["js", "jsx", "json", "node"], // Include .jsx files


};